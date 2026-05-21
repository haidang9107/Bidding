package org.example.server.controller;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Response;
import org.example.server.service.user.admin.AdminService;
import org.example.util.FileLogger;

import java.util.List;

/**
 * Controller for handling administrative tasks.
 * Refactored to delegate business logic to AdminService.
 */
public class AdminController {
    private final AdminService adminService;

    public AdminController() {
        this.adminService = new AdminService();
    }

    /**
     * Retrieves all users in the system.
     */
    public Response<List<User>> handleGetAllUsers() {
        try {
            List<User> users = adminService.getAllUsers();
            return new Response<>(MessageType.SUCCESS, true, "Users fetched successfully", users);
        } catch (Exception e) {
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
        
        try {
            // Controller handles data parsing/transformation
            String[] parts = payload.toString().split(":");
            if (parts.length < 2) return new Response<>(MessageType.ERROR, false, "Invalid format. Use accountname:status", null);
            
            String accountname = parts[0];
            int status = Integer.parseInt(parts[1]);

            // Delegate to Service
            boolean success = adminService.updateUserStatus(accountname, status);
            
            if (success) {
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

        try {
            int productId = Integer.parseInt(payload.toString());
            
            // Delegate to Service
            boolean success = adminService.cancelAuction(productId);
            
            if (success) {
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
