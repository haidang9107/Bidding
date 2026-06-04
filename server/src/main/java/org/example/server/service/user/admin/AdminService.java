package org.example.server.service.user.admin;

import org.example.dto.response.PagedResponse;
import org.example.model.Auction;
import org.example.model.user.User;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;
import org.example.server.service.auction.AuctionService;
import org.example.dto.response.AdminStatsResponse;
import org.example.model.enums.AuctionStatus;
import org.example.server.repository.AuctionDao;
import org.example.server.repository.ProductDao;
import org.example.server.repository.TransactionDao;
import org.example.util.FileLogger;
import org.example.server.event.EventPublisher;

import java.util.List;

/**
 * Service for handling administrative business logic. Delegates auction operations
 * to {@link AuctionService} rather than touching auction DAOs directly.
 */
public class AdminService {
    private final UserDao userDao;
    private final AuctionDao auctionDao;
    private final ProductDao productDao;
    private final TransactionDao transactionDao;
    private final AuctionService auctionService;
    private final TransactionManager txManager;
    private final EventPublisher eventPublisher;

    /**
     * Constructs an AdminService.
     * @param txManager      The transaction manager.
     * @param auctionService The auction service used for auction-related admin actions.
     * @param eventPublisher The event publisher for notifications.
     */
    public AdminService(TransactionManager txManager, AuctionService auctionService, EventPublisher eventPublisher) {
        this.userDao = UserDao.getInstance();
        this.auctionDao = AuctionDao.getInstance();
        this.productDao = ProductDao.getInstance();
        this.transactionDao = TransactionDao.getInstance();
        this.auctionService = auctionService;
        this.txManager = txManager;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Gathers system-wide statistics for the administrator.
     * @return The statistics response.
     */
    public AdminStatsResponse getSystemStats() {
        return txManager.query(conn -> {
            long totalUsers = userDao.getTotalUsersCount(conn);
            long activeUsers = userDao.countUsersByStatus(conn, 0);
            long bannedUsers = userDao.countUsersByStatus(conn, 1);
            
            long totalProducts = productDao.getTotalProductsCount(conn);
            long productsInInventory = productDao.countProductsByAuctionFlag(conn, false);
            long productsInAuction = productDao.countProductsByAuctionFlag(conn, true);
            
            long activeAuctions = auctionDao.countByStatus(conn, AuctionStatus.RUNNING);
            long completedAuctions = auctionDao.countByStatus(conn, AuctionStatus.FINISHED);
            long canceledAuctions = auctionDao.countByStatus(conn, AuctionStatus.CANCELED);
            
            long totalTransactions = transactionDao.getGlobalTotalTransactionsCount(conn);
            long totalVolume = transactionDao.getTotalTransactionVolume(conn);
            
            return new AdminStatsResponse(
                totalUsers, activeUsers, bannedUsers,
                totalProducts, productsInInventory, productsInAuction,
                activeAuctions, completedAuctions, canceledAuctions,
                totalTransactions, totalVolume
            );
        });
    }

    /**
     * Retrieves all users in the system.
     * @return List of all users.
     */
    public List<User> getAllUsers() {
        return txManager.query(userDao::findAllUsers);
    }

    /**
     * Retrieves a paged list of users.
     * @param page The page number (1-based).
     * @param pageSize The number of users per page.
     * @return A paged response containing users and metadata.
     */
    public PagedResponse<User> getUsersPaged(int page, int pageSize) {
        return txManager.query(conn -> {
            long totalItems = userDao.getTotalUsersCount(conn);
            List<User> users = userDao.getUsersPaged(conn, pageSize, (page - 1) * pageSize);
            return new PagedResponse<>(users, totalItems, page, pageSize);
        });
    }

    /**
     * Updates the status (e.g., active/banned) of a user.
     * @param accountname The account name of the user.
     * @param status The new status (0 for active, 1 for banned).
     * @return True if successful.
     */
    public boolean updateUserStatus(String accountname, int status) {
        return txManager.execute(conn -> {
            boolean success = userDao.updateUserStatus(conn, accountname, status);
            if (success) {
                String action = (status == 1) ? "BANNED" : "UNBANNED";
                FileLogger.info("Admin action: User " + accountname + " has been " + action);

                if (status == 1) {
                    cleanupBannedUser(conn, accountname);
                }
            }
            return success;
        });
    }

    private void cleanupBannedUser(java.sql.Connection conn, String accountname) {
        try {
            List<Auction> winningAuctions = auctionDao.findAuctionsByWinner(conn, accountname);
            if (winningAuctions.isEmpty()) return;

            FileLogger.info("Cleaning up " + winningAuctions.size() + " active bids for banned user: " + accountname + " (Option 2: Keep Price, Remove Winner)");
            for (Auction auction : winningAuctions) {
                // 1. Remove as winner but KEEP current price
                auctionDao.updateWinner(conn, auction.getAuctionId(), null);
                
                // 2. Refund blocked balance to balance
                long refundAmount = auction.getCurrentPrice();
                userDao.addBlockedBalance(conn, accountname, -refundAmount);
                userDao.addBalance(conn, accountname, refundAmount);
                
                FileLogger.info("Auction " + auction.getAuctionId() + ": Removed winner " + accountname + ", kept price " + refundAmount + ", and refunded balance.");

                // 3. Notify room subscribers that the winner has changed (to null)
                eventPublisher.publish(new org.example.server.event.NewBidPlacedEvent(
                    auction.getAuctionId(),
                    null,             // New winner is null
                    accountname,      // Old winner was the banned user
                    auction.getCurrentPrice(),
                    false,           // Not an auto-bid
                    auction.getEndTime()
                ));
            }
        } catch (java.sql.SQLException e) {
            FileLogger.error("Failed to cleanup banned user " + accountname, e);
        }
    }

    /**
     * Cancels an auction by delegating to the auction service.
     * @param auctionId The ID of the auction to cancel.
     * @return True if successful.
     */
    public boolean cancelAuction(int auctionId) {
        return auctionService.cancelAuction(auctionId);
    }
}
