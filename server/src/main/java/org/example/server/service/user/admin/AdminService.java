package org.example.server.service.user.admin;

import org.example.dto.response.PagedResponse;
import org.example.model.enums.AuctionStatus;
import org.example.model.product.Item;
import org.example.model.user.User;
import org.example.server.event.AuctionEndedEvent;
import org.example.server.event.EventPublisher;
import org.example.server.repository.ProductDao;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.util.List;

/**
 * Service for handling administrative business logic.
 * Refactored to use TransactionManager and EventPublisher.
 */
public class AdminService {
    private final UserDao userDao;
    private final ProductDao productDao;
    private final TransactionManager txManager;
    private final EventPublisher eventPublisher;

    /**
     * Constructs a new AdminService.
     * @param txManager The transaction manager.
     * @param eventPublisher The event publisher.
     */
    public AdminService(TransactionManager txManager, EventPublisher eventPublisher) {
        this.userDao = new UserDao();
        this.productDao = new ProductDao();
        this.txManager = txManager;
        this.eventPublisher = eventPublisher;
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
            }
            return success;
        });
    }

    /**
     * Cancels an auction and releases any blocked balances.
     * @param auctionId The ID of the auction to cancel.
     * @return True if successful.
     */
    public boolean cancelAuction(int auctionId) {
        return txManager.execute(conn -> {
            Item item = productDao.getAuctionForUpdate(conn, auctionId);
            if (item == null || item.getStatus() == AuctionStatus.FINISHED || item.getStatus() == AuctionStatus.CANCELED) {
                return false;
            }

            boolean success = productDao.updateStatus(conn, auctionId, AuctionStatus.CANCELED);
            if (success) {
                productDao.updateProductAuctionFlag(conn, item.getProductId(), false);
                if (item.getWinnerAccountname() != null) {
                    userDao.addBlockedBalance(conn, item.getWinnerAccountname(), -item.getCurrentPrice());
                }
                FileLogger.info("Admin action: Auction " + auctionId + " has been CANCELED.");
                eventPublisher.publish(new AuctionEndedEvent(auctionId, item.getName(), item.getWinnerAccountname(), item.getCurrentPrice(), true));
            }
            return success;
        });
    }
}