package org.example.server.service.user.admin;

import org.example.dto.PagedResponse;
import org.example.model.enums.AuctionStatus;
import org.example.model.product.Item;
import org.example.model.user.User;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.ProductDao;
import org.example.server.repository.UserDao;
import org.example.server.network.Broadcaster;
import org.example.payload.Response;
import org.example.model.enums.MessageType;
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
     * Retrieves users with pagination.
     */
    public PagedResponse<User> getUsersPaged(int page, int pageSize) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            long totalItems = userDao.getTotalUsersCount(conn);
            List<User> users = userDao.getUsersPaged(conn, pageSize, (page - 1) * pageSize);
            return new PagedResponse<>(users, totalItems, page, pageSize);
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
    public boolean cancelAuction(int auctionId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            Item item = productDao.getAuctionForUpdate(conn, auctionId);
            if (item == null) {
                conn.rollback();
                return false;
            }

            boolean success = productDao.updateStatus(conn, auctionId, AuctionStatus.CANCELED);
            if (success) {
                productDao.updateProductAuctionFlag(conn, item.getProductId(), false);
                if (item.getWinnerAccountname() != null) {
                    userDao.addBlockedBalance(conn, item.getWinnerAccountname(), -item.getCurrentPrice());
                }
                conn.commit();
                FileLogger.info("Admin action: Auction " + auctionId + " has been CANCELED.");
                Broadcaster.broadcastToAuction(auctionId, new Response<>(
                        MessageType.NOTIFICATION,
                        true,
                        "Auction " + auctionId + " has been canceled.",
                        null
                ));
            } else {
                conn.rollback();
            }
            return success;
        }
    }
}
