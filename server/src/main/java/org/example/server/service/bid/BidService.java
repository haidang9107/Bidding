package org.example.server.service.bid;

import org.example.dto.response.BidResult;
import org.example.dto.response.PagedResponse;
import org.example.model.Auction;
import org.example.model.AutoBid;
import org.example.model.Bid;
import org.example.model.enums.AuctionStatus;
import org.example.model.user.Member;
import org.example.model.user.User;
import org.example.server.event.EventPublisher;
import org.example.server.event.NewBidPlacedEvent;
import org.example.server.exception.BaseAppException;
import org.example.server.exception.FinanceException;
import org.example.server.exception.NotFoundException;
import org.example.server.exception.ValidationException;
import org.example.server.repository.AuctionDao;
import org.example.server.repository.AutoBidDao;
import org.example.server.repository.BidDao;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;
import org.example.server.service.auction.AntiSnipping;
import org.example.server.service.auction.AuctionMonitor;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Service for safe manual and automatic bid placement on auction sessions.
 */
public class BidService {
    private final AuctionDao auctionDao;
    private final BidDao bidDao;
    private final UserDao userDao;
    private final AutoBidDao autoBidDao;
    private final TransactionManager txManager;
    private final EventPublisher eventPublisher;
    private final AuctionMonitor auctionMonitor;

    /**
     * Constructs a new BidService.
     * @param txManager       The transaction manager.
     * @param eventPublisher  The event publisher.
     * @param auctionMonitor  The auction monitor (re-scheduled when anti-snipping fires).
     */
    public BidService(TransactionManager txManager, EventPublisher eventPublisher, AuctionMonitor auctionMonitor) {
        this.auctionDao = new AuctionDao();
        this.bidDao = new BidDao();
        this.userDao = new UserDao();
        this.autoBidDao = new AutoBidDao();
        this.txManager = txManager;
        this.eventPublisher = eventPublisher;
        this.auctionMonitor = auctionMonitor;
    }

    /**
     * Places a manual bid on an auction.
     * @param auctionId          The auction ID.
     * @param bidderAccountname  The bidder's account name.
     * @param bidAmount          The bid amount.
     * @return The bid result.
     */
    public BidResult placeBid(int auctionId, String bidderAccountname, long bidAmount) {
        if (auctionId <= 0) throw new ValidationException("Invalid Auction ID");
        if (bidAmount <= 0) throw new ValidationException("Bid amount must be positive");

        return txManager.execute(connection -> {
            Auction auction = loadActiveAuction(connection, auctionId);
            if (auction == null) {
                throw new NotFoundException("Auction not found or not active");
            }

            applyBid(connection, auction, bidderAccountname, bidAmount, false);
            boolean autoBidApplied = runAutoBidding(connection, auction);

            FileLogger.info("Bid sequence completed: Auction " + auctionId
                    + ", winner " + auction.getWinnerAccountname()
                    + ", price " + auction.getCurrentPrice());

            // Re-schedule the end in case anti-snipping extended it
            auctionMonitor.scheduleAuctionEnd(auctionId, auction.getEndTime());

            eventPublisher.publish(new NewBidPlacedEvent(
                    auctionId, auction.getWinnerAccountname(),
                    auction.getCurrentPrice(), autoBidApplied, auction.getEndTime()));

            return new BidResult(auctionId, auction.getWinnerAccountname(),
                    auction.getCurrentPrice(), autoBidApplied, auction.getEndTime());
        });
    }

    /**
     * Sets up or updates an automatic bidding configuration.
     * @param auctionId          The auction ID.
     * @param bidderAccountname  The user's account name.
     * @param maxBid             The maximum amount to bid.
     * @param incrementAmount    The increment step.
     */
    public void configureAutoBid(int auctionId, String bidderAccountname, long maxBid,
                                 long incrementAmount) {
        if (auctionId <= 0) throw new ValidationException("Auction ID is required.");
        if (maxBid <= 0) throw new ValidationException("Max bid must be greater than 0.");
        if (incrementAmount <= 0) throw new ValidationException("Increment amount must be greater than 0.");

        txManager.run(connection -> {
            Auction auction = loadActiveAuction(connection, auctionId);
            if (auction == null) {
                throw new NotFoundException("Auction not found or not active.");
            }

            User bidder = userDao.findByAccountnameForUpdate(connection, bidderAccountname);
            if (!(bidder instanceof Member member)) {
                throw new ValidationException("Only members can configure auto bidding.");
            }
            if (bidder.getStatus() != 0) {
                throw new ValidationException("User is not active.");
            }
            if (auction.getSellerAccountname().equals(bidderAccountname)) {
                throw new ValidationException("Seller cannot auto bid on their own item.");
            }

            long minimumNextBid = auction.getCurrentPrice() + auction.getStepPrice();
            if (maxBid < minimumNextBid) {
                throw new ValidationException("Max bid is too low. Minimum required: " + minimumNextBid);
            }
            if (incrementAmount < auction.getStepPrice()) {
                throw new ValidationException("Auto-bid increment (" + incrementAmount
                        + ") must be at least the auction step price (" + auction.getStepPrice() + ")");
            }

            long currentBidAmount = bidderAccountname.equals(auction.getWinnerAccountname())
                    ? auction.getCurrentPrice() : 0;
            long totalAvailable = member.getBalance() - (member.getBlockedBalance() - currentBidAmount);
            if (totalAvailable < minimumNextBid) {
                throw new FinanceException("Insufficient balance. Available: " + totalAvailable);
            }

            autoBidDao.upsertAutoBid(connection, auctionId, bidderAccountname, maxBid, incrementAmount);
            boolean autoBidApplied = runAutoBidding(connection, auction);

            FileLogger.info("Auto bid configured: User " + bidderAccountname
                    + " on Auction " + auctionId + " max " + maxBid);

            if (autoBidApplied) {
                auctionMonitor.scheduleAuctionEnd(auctionId, auction.getEndTime());
                eventPublisher.publish(new NewBidPlacedEvent(
                        auctionId, auction.getWinnerAccountname(),
                        auction.getCurrentPrice(), true, auction.getEndTime()));
            }
        });
    }

    /**
     * Cancels an automatic bidding configuration.
     * @param auctionId          The auction ID.
     * @param bidderAccountname  The user's account name.
     */
    public void cancelAutoBid(int auctionId, String bidderAccountname) {
        txManager.run(connection -> {
            boolean success = autoBidDao.deactivateAutoBid(connection, auctionId, bidderAccountname);
            if (!success) {
                throw new NotFoundException("Auto bid not found.");
            }
        });
    }

    /**
     * Retrieves a paged list of bids for an auction.
     * @param auctionId The auction ID.
     * @param page      The 1-based page number.
     * @param pageSize  The page size.
     * @return The paged bid history.
     */
    public PagedResponse<Bid> getBidHistoryPaged(int auctionId, int page, int pageSize) {
        return txManager.query(conn -> {
            long totalItems = bidDao.getTotalBidsCount(conn, auctionId);
            List<Bid> bids = bidDao.getBidsPaged(conn, auctionId, pageSize, (page - 1) * pageSize);
            return new PagedResponse<>(bids, totalItems, page, pageSize);
        });
    }

    /**
     * Loads an active auction (status RUNNING and not yet ended).
     */
    private Auction loadActiveAuction(Connection connection, int auctionId) throws SQLException {
        Auction auction = auctionDao.getAuctionForUpdate(connection, auctionId);
        if (auction == null) return null;
        if (auction.getStatus() != AuctionStatus.RUNNING) return null;

        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (auction.getEndTime() == null || !auction.getEndTime().after(now)) return null;
        return auction;
    }

    private void applyBid(Connection connection, Auction auction, String bidderAccountname,
                          long bidAmount, boolean autoBid) throws SQLException {
        String oldWinnerAccount = auction.getWinnerAccountname();
        User bidder = lockBidderAndPreviousWinner(connection, bidderAccountname, oldWinnerAccount);
        if (bidder == null) throw new NotFoundException("User not found.");
        if (bidder.getStatus() != 0) throw new ValidationException("User is not active.");
        if (!(bidder instanceof Member member)) throw new ValidationException("Only members can place bids.");
        if (auction.getSellerAccountname().equals(bidderAccountname)) {
            throw new ValidationException("Seller cannot bid on their own item.");
        }

        long minimumBid = auction.getCurrentPrice() + auction.getStepPrice();
        if (bidAmount < minimumBid) {
            throw new ValidationException("Bid amount is too low. Minimum required: " + minimumBid);
        }

        // Buy Now Logic: Clamp the bid amount to the Buy Now price if it's met or exceeded
        boolean isBuyNow = false;
        long finalBidAmount = bidAmount;
        if (auction.getBuyNowPrice() != null && bidAmount >= auction.getBuyNowPrice()) {
            finalBidAmount = auction.getBuyNowPrice();
            isBuyNow = true;
        }

        long extraBlocked = oldWinnerAccount != null && oldWinnerAccount.equals(bidderAccountname)
                ? finalBidAmount - auction.getCurrentPrice()
                : finalBidAmount;
        long availableBalance = member.getBalance() - member.getBlockedBalance();
        if (extraBlocked > availableBalance) {
            throw new FinanceException("Insufficient balance. Available: " + availableBalance);
        }

        if (oldWinnerAccount != null && !oldWinnerAccount.equals(bidderAccountname)) {
            userDao.addBlockedBalance(connection, oldWinnerAccount, -auction.getCurrentPrice());
        }
        userDao.addBlockedBalance(connection, bidderAccountname, extraBlocked);
        auctionDao.updateBidLocked(connection, auction.getAuctionId(), finalBidAmount, bidderAccountname);
        bidDao.insertBid(connection, auction.getAuctionId(), bidderAccountname, finalBidAmount, autoBid);

        auction.setCurrentPrice(finalBidAmount);
        auction.setWinnerAccountname(bidderAccountname);

        if (isBuyNow) {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            auction.setEndTime(now);
            auctionDao.updateAuctionEndTime(connection, auction.getAuctionId(), now);
            FileLogger.info("Buy Now triggered for Auction " + auction.getAuctionId() + " by " + bidderAccountname);
        } else {
            AntiSnipping.process(connection, auction, auctionDao);
        }
    }

    private boolean runAutoBidding(Connection connection, Auction auction) throws SQLException {
        boolean appliedAtLeastOnce = false;

        List<AutoBid> activeBids = autoBidDao.findAllActiveForAuction(connection, auction.getAuctionId());

        while (!activeBids.isEmpty()) {
            // If Buy Now was triggered, the auction end time will be set to now or past
            if (auction.getEndTime() != null && auction.getEndTime().getTime() <= System.currentTimeMillis()) {
                break;
            }

            AutoBid highest = activeBids.get(0);
            AutoBid secondHighest = activeBids.size() > 1 ? activeBids.get(1) : null;

            long minimumNextBid = auction.getCurrentPrice() + auction.getStepPrice();
            long targetPrice;

            if (secondHighest != null) {
                long competePrice = secondHighest.getMaxBid() + auction.getStepPrice();
                targetPrice = Math.min(highest.getMaxBid(), competePrice);
                if (!highest.getBidderAccountname().equals(auction.getWinnerAccountname())) {
                    targetPrice = Math.max(targetPrice, minimumNextBid);
                }
            } else {
                if (highest.getBidderAccountname().equals(auction.getWinnerAccountname())) {
                    break;
                }
                targetPrice = auction.getCurrentPrice() + highest.getIncrementAmount();
                if (targetPrice > highest.getMaxBid()) {
                    targetPrice = highest.getMaxBid();
                }
                targetPrice = Math.max(targetPrice, minimumNextBid);
            }

            if (targetPrice > highest.getMaxBid() || targetPrice < minimumNextBid) {
                if (highest.getBidderAccountname().equals(auction.getWinnerAccountname())) {
                    break;
                }
                break;
            }

            try {
                applyBid(connection, auction, highest.getBidderAccountname(), targetPrice, true);
                appliedAtLeastOnce = true;
                break;
            } catch (BaseAppException e) {
                autoBidDao.deactivateAutoBid(connection, auction.getAuctionId(), highest.getBidderAccountname());
                FileLogger.error("Auto bid deactivated for " + highest.getBidderAccountname()
                        + " due to error: " + e.getMessage(), e);
                activeBids.remove(0);
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
