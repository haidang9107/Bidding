package org.example.server.service.bid;

import org.example.dto.response.BidResult;
import org.example.model.AutoBid;
import org.example.model.enums.AuctionStatus;
import org.example.model.product.Item;
import org.example.model.user.Member;
import org.example.model.user.User;
import org.example.server.exception.AuctionException;
import org.example.server.exception.BaseAppException;
import org.example.server.exception.FinanceException;
import org.example.server.exception.NotFoundException;
import org.example.server.exception.ValidationException;
import org.example.server.repository.AuctionDao;
import org.example.server.repository.AutoBidDao;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.ProductDao;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Service for safe manual and automatic bid placement on auction sessions.
 */
public class BidService {
    private final ProductDao productDao;
    private final UserDao userDao;
    private final AuctionDao auctionDao;
    private final AutoBidDao autoBidDao;

    public BidService() {
        this.productDao = new ProductDao();
        this.userDao = new UserDao();
        this.auctionDao = new AuctionDao();
        this.autoBidDao = new AutoBidDao();
    }

    public BidResult placeBid(int auctionId, String bidderAccountname, long bidAmount) {
        if (auctionId <= 0) throw new ValidationException("Invalid Auction ID");
        if (bidAmount <= 0) throw new ValidationException("Bid amount must be positive");

        try (Connection connection = DatabaseManager.getConnection()) {
            try {
                connection.setAutoCommit(false);

                Item item = loadActiveAuction(connection, auctionId);
                if (item == null) {
                    throw new NotFoundException("Auction not found or not active");
                }

                applyBid(connection, item, bidderAccountname, bidAmount, false);

                item.setCurrentPrice(bidAmount);
                item.setWinnerAccountname(bidderAccountname);

                boolean autoBidApplied = runAutoBidding(connection, item);

                connection.commit();
                FileLogger.info("Bid sequence completed: Auction " + auctionId
                        + ", winner " + item.getWinnerAccountname()
                        + ", price " + item.getCurrentPrice());
                return new BidResult(auctionId, item.getWinnerAccountname(),
                        item.getCurrentPrice(), autoBidApplied, item.getEndTime());
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                }
                throw new AuctionException("Database error during bidding: " + e.getMessage());
            }
        } catch (SQLException e) {
            FileLogger.error("Bidding error", e);
            throw new AuctionException("Internal server error during bidding");
        }
    }

    public void configureAutoBid(int auctionId, String bidderAccountname, long maxBid,
                                   long incrementAmount) {
        if (auctionId <= 0) throw new ValidationException("Auction ID is required.");
        if (maxBid <= 0) throw new ValidationException("Max bid must be greater than 0.");
        if (incrementAmount <= 0) throw new ValidationException("Increment amount must be greater than 0.");

        try (Connection connection = DatabaseManager.getConnection()) {
            try {
                connection.setAutoCommit(false);

                Item item = loadActiveAuction(connection, auctionId);
                if (item == null) {
                    throw new NotFoundException("Auction not found or not active.");
                }

                User bidder = userDao.findByAccountnameForUpdate(connection, bidderAccountname);
                if (!(bidder instanceof Member member)) {
                    throw new ValidationException("Only members can configure auto bidding.");
                }
                if (bidder.getStatus() != 0) {
                    throw new ValidationException("User is not active.");
                }
                if (item.getSellerAccountname().equals(bidderAccountname)) {
                    throw new ValidationException("Seller cannot auto bid on their own item.");
                }

                long minimumNextBid = item.getCurrentPrice() + item.getStepPrice();
                if (maxBid < minimumNextBid) {
                    throw new ValidationException("Max bid is too low. Minimum required: " + minimumNextBid);
                }
                long availableBalance = member.getBalance() - member.getBlockedBalance();
                if (availableBalance < minimumNextBid
                        && !bidderAccountname.equals(item.getWinnerAccountname())) {
                    throw new FinanceException("Insufficient balance. Available: " + availableBalance);
                }

                autoBidDao.upsertAutoBid(connection, auctionId, bidderAccountname, maxBid, incrementAmount);
                runAutoBidding(connection, item);

                connection.commit();
                FileLogger.info("Auto bid configured: User " + bidderAccountname
                        + " on Auction " + auctionId + " max " + maxBid);
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                }
                throw new AuctionException("Database error: " + e.getMessage());
            }
        } catch (SQLException e) {
            FileLogger.error("Auto bid configuration error", e);
            throw new AuctionException("Internal error: " + e.getMessage());
        }
    }

    public void cancelAutoBid(int auctionId, String bidderAccountname) {
        try (Connection connection = DatabaseManager.getConnection()) {
            boolean success = autoBidDao.deactivateAutoBid(connection, auctionId, bidderAccountname);
            if (!success) {
                throw new NotFoundException("Auto bid not found.");
            }
        } catch (SQLException e) {
            FileLogger.error("Auto bid cancel error", e);
            throw new AuctionException("Internal error: " + e.getMessage());
        }
    }

    private Item loadActiveAuction(Connection connection, int auctionId) throws SQLException {
        Item item = productDao.getAuctionForUpdate(connection, auctionId);
        if (item == null) return null;
        if (item.getStatus() != AuctionStatus.ACTIVE) return null;

        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (item.getEndTime() == null || !item.getEndTime().after(now)) return null;
        return item;
    }

    private void applyBid(Connection connection, Item item, String bidderAccountname,
                            long bidAmount, boolean autoBid) throws SQLException {
        String oldWinnerAccount = item.getWinnerAccountname();
        User bidder = lockBidderAndPreviousWinner(connection, bidderAccountname, oldWinnerAccount);
        if (bidder == null) throw new NotFoundException("User not found.");
        if (bidder.getStatus() != 0) throw new ValidationException("User is not active.");
        if (!(bidder instanceof Member member)) throw new ValidationException("Only members can place bids.");
        if (item.getSellerAccountname().equals(bidderAccountname)) {
            throw new ValidationException("Seller cannot bid on their own item.");
        }

        long minimumBid = item.getCurrentPrice() + item.getStepPrice();
        if (bidAmount < minimumBid) throw new ValidationException("Bid amount is too low. Minimum required: " + minimumBid);

        long extraBlocked = oldWinnerAccount != null && oldWinnerAccount.equals(bidderAccountname)
                ? bidAmount - item.getCurrentPrice()
                : bidAmount;
        long availableBalance = member.getBalance() - member.getBlockedBalance();
        if (extraBlocked > availableBalance) {
            throw new FinanceException("Insufficient balance. Available: " + availableBalance);
        }

        if (oldWinnerAccount != null && !oldWinnerAccount.equals(bidderAccountname)) {
            userDao.addBlockedBalance(connection, oldWinnerAccount, -item.getCurrentPrice());
        }
        userDao.addBlockedBalance(connection, bidderAccountname, extraBlocked);
        productDao.updateBidLocked(connection, item.getAuctionId(), bidAmount, bidderAccountname);
        auctionDao.insertBid(connection, item.getAuctionId(), bidderAccountname, bidAmount, autoBid);

        item.setCurrentPrice(bidAmount);
        item.setWinnerAccountname(bidderAccountname);

        AntiSnipping.process(connection, item, productDao);
    }

    private boolean runAutoBidding(Connection connection, Item item) throws SQLException {
        boolean appliedAtLeastOnce = false;

        List<AutoBid> activeBids = autoBidDao.findAllActiveForAuction(connection, item.getAuctionId());

        while (!activeBids.isEmpty()) {
            AutoBid highest = activeBids.get(0);
            AutoBid secondHighest = activeBids.size() > 1 ? activeBids.get(1) : null;

            long minimumNextBid = item.getCurrentPrice() + item.getStepPrice();
            long targetPrice;

            if (secondHighest != null) {
                // Determine price based on competition
                long competePrice = secondHighest.getMaxBid() + item.getStepPrice();
                targetPrice = Math.min(highest.getMaxBid(), competePrice);
                
                // If highest is not winning, must at least meet minimumNextBid
                if (!highest.getBidderAccountname().equals(item.getWinnerAccountname())) {
                    targetPrice = Math.max(targetPrice, minimumNextBid);
                }
            } else {
                // Competing against manual bid
                if (highest.getBidderAccountname().equals(item.getWinnerAccountname())) {
                    break; // Already winning and no one is pushing
                }
                targetPrice = minimumNextBid;
            }

            // Safety check: targetPrice must be within highest's limit and above current
            if (targetPrice > highest.getMaxBid() || targetPrice < minimumNextBid) {
                // If current winner is already highest, we are done
                if (highest.getBidderAccountname().equals(item.getWinnerAccountname())) {
                    break;
                }
                // Otherwise, highest cannot beat current price
                break;
            }

            try {
                applyBid(connection, item, highest.getBidderAccountname(), targetPrice, true);
                appliedAtLeastOnce = true;
                break; // Target achieved
            } catch (BaseAppException e) {
                // If highest bidder has issues (e.g. balance), deactivate and re-evaluate
                autoBidDao.deactivateAutoBid(connection, item.getAuctionId(), highest.getBidderAccountname());
                FileLogger.error("Auto bid deactivated for " + highest.getBidderAccountname() + " due to error: " + e.getMessage(), e);
                activeBids.remove(0); // re-evaluate with remaining bids
            }
        }
        return appliedAtLeastOnce;
    }

    private User lockBidderAndPreviousWinner(Connection connection, String bidderAccountname,
                                             String oldWinnerAccount) throws SQLException {
        if (oldWinnerAccount == null || oldWinnerAccount.equals(bidderAccountname)) {
            return userDao.findByAccountnameForUpdate(connection, bidderAccountname);
        }

        User bidder;
        if (bidderAccountname.compareTo(oldWinnerAccount) < 0) {
            bidder = userDao.findByAccountnameForUpdate(connection, bidderAccountname);
            userDao.findByAccountnameForUpdate(connection, oldWinnerAccount);
        } else {
            userDao.findByAccountnameForUpdate(connection, oldWinnerAccount);
            bidder = userDao.findByAccountnameForUpdate(connection, bidderAccountname);
        }
        return bidder;
    }
}
