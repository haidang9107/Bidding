package org.example.server.service.bid;

import org.example.dto.response.BidResult;
import org.example.dto.response.PagedResponse;
import org.example.model.AutoBid;
import org.example.model.Bid;
import org.example.model.enums.AuctionStatus;
import org.example.model.product.Item;
import org.example.model.user.Member;
import org.example.model.user.User;
import org.example.server.event.EventPublisher;
import org.example.server.event.NewBidPlacedEvent;
import org.example.server.exception.AuctionException;
import org.example.server.exception.BaseAppException;
import org.example.server.exception.FinanceException;
import org.example.server.exception.NotFoundException;
import org.example.server.exception.ValidationException;
import org.example.server.repository.AuctionDao;
import org.example.server.repository.AutoBidDao;
import org.example.server.repository.ProductDao;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;
import org.example.server.network.AuctionMonitor;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Service for safe manual and automatic bid placement on auction sessions.
 * Refactored to use TransactionManager, EventPublisher, and AuctionMonitor.
 */
public class BidService {
    private final ProductDao productDao;
    private final UserDao userDao;
    private final AuctionDao auctionDao;
    private final AutoBidDao autoBidDao;
    private final TransactionManager txManager;
    private final EventPublisher eventPublisher;
    private final AuctionMonitor auctionMonitor;

    /**
     * Constructs a new BidService.
     * @param txManager The transaction manager.
     * @param eventPublisher The event publisher.
     * @param auctionMonitor The auction monitor.
     */
    public BidService(TransactionManager txManager, EventPublisher eventPublisher, AuctionMonitor auctionMonitor) {
        this.productDao = new ProductDao();
        this.userDao = new UserDao();
        this.auctionDao = new AuctionDao();
        this.autoBidDao = new AutoBidDao();
        this.txManager = txManager;
        this.eventPublisher = eventPublisher;
        this.auctionMonitor = auctionMonitor;
    }

    /**
     * Places a manual bid on an auction.
     * @param auctionId The ID of the auction.
     * @param bidderAccountname The account name of the bidder.
     * @param bidAmount The amount of the bid.
     * @return The result of the bidding process.
     */
    public BidResult placeBid(int auctionId, String bidderAccountname, long bidAmount) {
        if (auctionId <= 0) throw new ValidationException("Invalid Auction ID");
        if (bidAmount <= 0) throw new ValidationException("Bid amount must be positive");

        return txManager.execute(connection -> {
            Item item = loadActiveAuction(connection, auctionId);
            if (item == null) {
                throw new NotFoundException("Auction not found or not active");
            }

            applyBid(connection, item, bidderAccountname, bidAmount, false);

            boolean autoBidApplied = runAutoBidding(connection, item);

            FileLogger.info("Bid sequence completed: Auction " + auctionId
                    + ", winner " + item.getWinnerAccountname()
                    + ", price " + item.getCurrentPrice());
            
            // Đăng ký lịch kết thúc chính xác (đề phòng Anti-snipping kéo dài thời gian)
            auctionMonitor.scheduleAuctionEnd(auctionId, item.getEndTime());

            eventPublisher.publish(new NewBidPlacedEvent(
                    auctionId, item.getWinnerAccountname(),
                    item.getCurrentPrice(), autoBidApplied, item.getEndTime()));

            return new BidResult(auctionId, item.getWinnerAccountname(),
                    item.getCurrentPrice(), autoBidApplied, item.getEndTime());
        });
    }

    /**
     * Sets up or updates an automatic bidding configuration for a user.
     * @param auctionId The ID of the auction.
     * @param bidderAccountname The account name of the user.
     * @param maxBid The maximum amount to bid.
     * @param incrementAmount The increment step.
     */
    public void configureAutoBid(int auctionId, String bidderAccountname, long maxBid,
                                   long incrementAmount) {
        if (auctionId <= 0) throw new ValidationException("Auction ID is required.");
        if (maxBid <= 0) throw new ValidationException("Max bid must be greater than 0.");
        if (incrementAmount <= 0) throw new ValidationException("Increment amount must be greater than 0.");

        txManager.run(connection -> {
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
            
            // Logic: Bước giá của auto-bid phải >= bước giá tối thiểu của sản phẩm
            if (incrementAmount < item.getStepPrice()) {
                throw new ValidationException("Auto-bid increment (" + incrementAmount 
                    + ") must be at least the auction step price (" + item.getStepPrice() + ")");
            }

            long currentBidAmount = bidderAccountname.equals(item.getWinnerAccountname()) ? item.getCurrentPrice() : 0;
            long totalAvailable = member.getBalance() - (member.getBlockedBalance() - currentBidAmount);
            if (totalAvailable < minimumNextBid) {
                throw new FinanceException("Insufficient balance. Available: " + totalAvailable);
            }

            autoBidDao.upsertAutoBid(connection, auctionId, bidderAccountname, maxBid, incrementAmount);
            boolean autoBidApplied = runAutoBidding(connection, item);

            FileLogger.info("Auto bid configured: User " + bidderAccountname
                    + " on Auction " + auctionId + " max " + maxBid);

            if (autoBidApplied) {
                // Cập nhật lại lịch kết thúc nếu tự động bid làm thay đổi thời gian (Anti-snipping)
                auctionMonitor.scheduleAuctionEnd(auctionId, item.getEndTime());

                eventPublisher.publish(new NewBidPlacedEvent(
                        auctionId, item.getWinnerAccountname(),
                        item.getCurrentPrice(), true, item.getEndTime()));
            }
        });
    }

    /**
     * Cancels an automatic bidding configuration.
     * @param auctionId The ID of the auction.
     * @param bidderAccountname The account name of the user.
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
     * Retrieves a paged list of bids for a specific auction.
     * @param auctionId The ID of the auction.
     * @param page The page number (1-based).
     * @param pageSize The number of items per page.
     * @return A paged response containing the bids.
     */
    public PagedResponse<Bid> getBidHistoryPaged(int auctionId, int page, int pageSize) {
        return txManager.query(conn -> {
            long totalItems = auctionDao.getTotalBidsCount(conn, auctionId);
            List<Bid> bids = auctionDao.getBidsPaged(conn, auctionId, pageSize, (page - 1) * pageSize);
            return new PagedResponse<>(bids, totalItems, page, pageSize);
        });
    }

    /**
     * Loads an active auction and verifies it's still running.
     * @param connection The database connection.
     * @param auctionId The auction ID.
     * @return The auction item, or null if not found/not active.
     * @throws SQLException If a database error occurs.
     */
    private Item loadActiveAuction(Connection connection, int auctionId) throws SQLException {
        Item item = productDao.getAuctionForUpdate(connection, auctionId);
        if (item == null) return null;
        if (item.getStatus() != AuctionStatus.RUNNING) return null;

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
                long competePrice = secondHighest.getMaxBid() + item.getStepPrice();
                targetPrice = Math.min(highest.getMaxBid(), competePrice);
                if (!highest.getBidderAccountname().equals(item.getWinnerAccountname())) {
                    targetPrice = Math.max(targetPrice, minimumNextBid);
                }
            } else {
                if (highest.getBidderAccountname().equals(item.getWinnerAccountname())) {
                    break; 
                }
                
                // Sử dụng chính xác bước giá người dùng đã cài đặt (đã được validation ở Bước 1)
                targetPrice = item.getCurrentPrice() + highest.getIncrementAmount();
                
                // Đảm bảo không vượt quá mức tối đa người dùng cho phép
                if (targetPrice > highest.getMaxBid()) {
                    targetPrice = highest.getMaxBid();
                }

                // Đảm bảo vẫn phải đáp ứng bước giá tối thiểu của sàn (phòng hờ)
                targetPrice = Math.max(targetPrice, minimumNextBid);
            }

            if (targetPrice > highest.getMaxBid() || targetPrice < minimumNextBid) {
                if (highest.getBidderAccountname().equals(item.getWinnerAccountname())) {
                    break;
                }
                break;
            }

            try {
                applyBid(connection, item, highest.getBidderAccountname(), targetPrice, true);
                appliedAtLeastOnce = true;
                break; 
            } catch (BaseAppException e) {
                autoBidDao.deactivateAutoBid(connection, item.getAuctionId(), highest.getBidderAccountname());
                FileLogger.error("Auto bid deactivated for " + highest.getBidderAccountname() + " due to error: " + e.getMessage(), e);
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