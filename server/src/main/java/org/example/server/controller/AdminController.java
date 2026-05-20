package org.example.server.controller;

import org.example.model.enums.AuctionStatus;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Response;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.ProductDao;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for handling administrative tasks.
 */
public class AdminController {
    private final UserDao userDao;
    private final ProductDao productDao;

    public AdminController() {
        this.userDao = new UserDao();
        this.productDao = new ProductDao();
    }

    /**
     * Retrieves all users in the system.
     */
    public Response<List<User>> handleGetAllUsers() {
        try (Connection conn = DatabaseManager.getConnection()) {
            List<User> users = userDao.findAllUsers(conn);
            return new Response<>(MessageType.SUCCESS, true, "Users fetched successfully", users);
        } catch (SQLException e) {
            FileLogger.error("Admin: Failed to fetch users", e);
            return new Response<>(MessageType.ERROR, false, "Internal error fetching users", null);
        }
    }

    /**
     * Bans or unbans a user.
     * Expects a payload containing the target userId and the new status.
     */
    public Response<String> handleBanUser(Object payload) {
        if (payload == null) return new Response<>(MessageType.ERROR, false, "Account name and status required", null);
        
        try (Connection conn = DatabaseManager.getConnection()) {
            // Simplification: Expecting format "accountname:status"
            String[] parts = payload.toString().split(":");
            String accountname = parts[0];
            int status = Integer.parseInt(parts[1]);

            boolean success = userDao.updateUserStatus(conn, accountname, status);
            if (success) {
                String action = (status == 1) ? "banned" : "unbanned";
                FileLogger.info("Admin action: User " + accountname + " has been " + action);
                return new Response<>(MessageType.SUCCESS, true, "User status updated successfully", null);
            } else {
                return new Response<>(MessageType.ERROR, false, "User not found or status already set", null);
            }
        } catch (Exception e) {
            FileLogger.error("Admin: Failed to update user status", e);
            return new Response<>(MessageType.ERROR, false, "Invalid format or internal error", null);
        }
    }

    /**
     * Cancels an ongoing auction.
     */
    public Response<String> handleCancelAuction(Object payload) {
        if (payload == null) return new Response<>(MessageType.ERROR, false, "Product ID required", null);

        try (Connection conn = DatabaseManager.getConnection()) {
            int productId = Integer.parseInt(payload.toString());
            boolean success = productDao.updateStatus(conn, productId, AuctionStatus.CANCELED);
            
            if (success) {
                FileLogger.info("Admin action: Auction " + productId + " has been CANCELED.");
                return new Response<>(MessageType.SUCCESS, true, "Auction canceled successfully", null);
            } else {
                return new Response<>(MessageType.ERROR, false, "Auction not found", null);
            }
        } catch (Exception e) {
            FileLogger.error("Admin: Failed to cancel auction", e);
            return new Response<>(MessageType.ERROR, false, "Invalid Product ID or internal error", null);
        }
    }
}
