package org.example.server.controller;

import org.example.dto.request.AdminUserControlRequest;
import org.example.dto.request.AuctionCancelRequest;
import org.example.dto.response.PagedResponse;
import org.example.dto.request.PaginationRequest;
import org.example.dto.response.UserResponse;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Response;
import org.example.server.service.user.admin.AdminService;
import org.example.util.FileLogger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for handling administrative tasks.
 */
public class AdminController {
    private final AdminService adminService;

    /**
     * Constructs an AdminController with the specified AdminService.
     *
     * @param adminService the admin service to use for administrative operations
     */
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Handles the request to fetch a paged list of all users.
     *
     * @param pagReq the pagination request details
     * @return a response containing the paged list of users
     */
    public Response<?> handleGetAllUsers(PaginationRequest pagReq) {
        try {
            if (pagReq == null) {
                pagReq = new PaginationRequest(1, 10);
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
     * Updates a user's status (e.g., bans a user).
     * @param request The control request containing target user and new status.
     * @return A success or error response.
     */
    public Response<String> handleBanUser(AdminUserControlRequest request) {
        if (request == null) return new Response<>(MessageType.ERROR, false, "AdminUserControlRequest required", null);
        
        try {
            String accountname = request.getTargetAccountname();
            int status = request.getStatus();

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
     * Handles the request to cancel an auction.
     *
     * @param request the auction cancel request details
     * @return a response indicating the result of the cancellation
     */
    public Response<String> handleCancelAuction(AuctionCancelRequest request) {
        if (request == null) return new Response<>(MessageType.ERROR, false, "AuctionCancelRequest required", null);

        try {
            int auctionId = request.getAuctionId();
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
