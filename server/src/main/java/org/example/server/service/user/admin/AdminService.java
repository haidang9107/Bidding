package org.example.server.service.user.admin;

import org.example.model.enums.AuctionStatus;
import org.example.model.user.User;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.ProductDao;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Service for handling administrative business logic.
 */
public class AdminService {
    private final UserDao userDao;
    private final ProductDao productDao;

    public AdminService() {
        this.userDao = new UserDao();
        this.productDao = new ProductDao();
    }

    /**
     * Retrieves all users.
     */
    public List<User> getAllUsers() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return userDao.findAllUsers(conn);
        }
    }

    /**
     * Updates user status (Ban/Unban).
     */
    public boolean updateUserStatus(String accountname, int status) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            boolean success = userDao.updateUserStatus(conn, accountname, status);
            if (success) {
                String action = (status == 1) ? "BANNED" : "UNBANNED";
                FileLogger.info("Admin action: User " + accountname + " has been " + action);
            }
            return success;
        }
    }

    /**
     * Cancels an auction.
     */
    public boolean cancelAuction(int productId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            boolean success = productDao.updateStatus(conn, productId, AuctionStatus.CANCELED);
            if (success) {
                FileLogger.info("Admin action: Auction " + productId + " has been CANCELED.");
            }
            return success;
        }
    }
}
