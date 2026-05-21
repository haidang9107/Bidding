package org.example.server.controller;

import org.example.dto.AdminUserControlRequest;
import org.example.dto.AuctionCancelRequest;
import org.example.dto.PagedResponse;
import org.example.dto.PaginationRequest;
import org.example.dto.UserResponse;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Response;
import org.example.server.service.user.admin.AdminService;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;

import java.util.List;
import java.util.stream.Collectors;

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
     * Retrieves users in the system with pagination.
     */
    public Response<?> handleGetAllUsers(Object payload) {
        try {
            PaginationRequest pagReq;
            if (payload == null) {
                pagReq = new PaginationRequest(1, 10);
            } else {
                pagReq = JsonConverter.fromJson(JsonConverter.toJson(payload), PaginationRequest.class);
                if (pagReq == null) pagReq = new PaginationRequest(1, 10);
            }

            PagedResponse<User> pagedUsers = adminService.getUsersPaged(pagReq.getPage(), pagReq.getPageSize());
            
            List<UserResponse> userResponses = pagedUsers.getItems().stream()
                    .map(UserResponse::new)
                    .collect(Collectors.toList());
            
            PagedResponse<UserResponse> finalResponse = new PagedResponse<>(
                userResponses,
                pagedUsers.getTotalItems(),
                pagedUsers.getCurrentPage(),
                pagedUsers.getPageSize()
            );

            return new Response<>(MessageType.SUCCESS, true, "Users fetched successfully", finalResponse);
        } catch (Exception e) {
            FileLogger.error("Admin: Failed to fetch users", e);
            return new Response<>(MessageType.ERROR, false, "Internal error fetching users", null);
        }
    }

    /**
     * Bans or unbans a user.
     * Expects an AdminUserControlRequest DTO.
     */
    public Response<String> handleBanUser(Object payload) {
        if (payload == null) return new Response<>(MessageType.ERROR, false, "AdminUserControlRequest required", null);
        
        try {
            AdminUserControlRequest request = JsonConverter.fromJson(JsonConverter.toJson(payload), AdminUserControlRequest.class);
            
            String accountname = request.getTargetAccountname();
            int status = request.getStatus();

            // Delegate to Service
            boolean success = adminService.updateUserStatus(accountname, status);
            
            if (success) {
                return new Response<>(MessageType.SUCCESS, true, "User status updated successfully", null);
            } else {
                return new Response<>(MessageType.ERROR, false, "User not found or status already set", null);
            }
        } catch (Exception e) {
            FileLogger.error("Admin: Failed to update user status", e);
            return new Response<>(MessageType.ERROR, false, "Invalid payload or internal error", null);
        }
    }

    /**
     * Cancels an ongoing auction.
     * Expects an AuctionCancelRequest DTO.
     */
    public Response<String> handleCancelAuction(Object payload) {
        if (payload == null) return new Response<>(MessageType.ERROR, false, "AuctionCancelRequest required", null);

        try {
            AuctionCancelRequest request = JsonConverter.fromJson(JsonConverter.toJson(payload), AuctionCancelRequest.class);
            int auctionId = request.getAuctionId();
            
            // Delegate to Service
            boolean success = adminService.cancelAuction(auctionId);
            
            if (success) {
                return new Response<>(MessageType.SUCCESS, true, "Auction canceled successfully", null);
            } else {
                return new Response<>(MessageType.ERROR, false, "Auction not found", null);
            }
        } catch (Exception e) {
            FileLogger.error("Admin: Failed to cancel auction", e);
            return new Response<>(MessageType.ERROR, false, "Invalid payload or internal error", null);
        }
    }
}
