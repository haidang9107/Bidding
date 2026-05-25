package org.example.server.service.user.admin;

import org.example.dto.response.PagedResponse;
import org.example.model.user.User;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;
import org.example.server.service.auction.AuctionService;
import org.example.util.FileLogger;

import java.util.List;

/**
 * Service for handling administrative business logic. Delegates auction operations
 * to {@link AuctionService} rather than touching auction DAOs directly.
 */
public class AdminService {
    private final UserDao userDao;
    private final AuctionService auctionService;
    private final TransactionManager txManager;

    /**
     * Constructs a new AdminService.
     * @param txManager      The transaction manager.
     * @param auctionService The auction service used for auction-related admin actions.
     */
    public AdminService(TransactionManager txManager, AuctionService auctionService) {
        this.userDao = new UserDao();
        this.auctionService = auctionService;
        this.txManager = txManager;
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
     * Cancels an auction by delegating to the auction service.
     * @param auctionId The ID of the auction to cancel.
     * @return True if successful.
     */
    public boolean cancelAuction(int auctionId) {
        return auctionService.cancelAuction(auctionId);
    }
}
