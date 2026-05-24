package org.example.server.controller;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Response;
import org.example.server.network.SessionManager;
import org.example.server.service.user.UserService;
import org.example.util.FileLogger;

import org.example.dto.request.UserProfileUpdateRequest;
import org.example.dto.response.UserResponse;

import java.nio.channels.SocketChannel;

/**
 * Controller for handling common user-related requests (non-auth).
 */
public class UserController {
    private final UserService userService;

    /**
     * Constructs a UserController with the specified UserService.
     *
     * @param userService the user service to use for user operations
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Handles the request to fetch the profile of the current user.
     *
     * @param channel the socket channel of the user
     * @return a response containing the user's profile details
     */
    public Response<UserResponse> handleGetProfile(SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        
        return new Response<>(MessageType.SUCCESS, true, "Profile fetched successfully", new UserResponse(currentUser));
    }

    /**
     * Updates the profile information of the current user.
     * @param request The profile update request.
     * @param channel The socket channel of the user.
     * @return A success or error response.
     */
    public Response<String> handleUpdateProfile(UserProfileUpdateRequest request, SocketChannel channel) {
        if (request == null) return new Response<>(MessageType.ERROR, false, "Update data required", null);
        
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) return new Response<>(MessageType.ERROR, false, "Unauthorized", null);

        try {
            boolean success = true;
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                success = userService.updateEmail(currentUser.getAccountname(), request.getEmail());
                if (success) currentUser.setEmail(request.getEmail());
            }
            
            if (success && request.getAvt() != null) {
                success = userService.updateAvatar(currentUser.getAccountname(), request.getAvt());
                if (success) currentUser.setAvt(request.getAvt());
            }
            
            if (success) {
                return new Response<>(MessageType.SUCCESS, true, "Profile updated successfully", null);
            } else {
                return new Response<>(MessageType.ERROR, false, "Failed to update profile", null);
            }
        } catch (Exception e) {
            FileLogger.error("Error updating profile", e);
            return new Response<>(MessageType.ERROR, false, "Internal server error", null);
        }
    }

    /**
     * Handles the request to update the avatar of the current user.
     *
     * @param avatarPath the new avatar path
     * @param channel the socket channel of the user
     * @return a response indicating the result of the avatar update
     */
    public Response<String> handleUpdateAvatar(String avatarPath, SocketChannel channel) {
        if (avatarPath == null || avatarPath.isBlank()) return new Response<>(MessageType.ERROR, false, "Avatar path required", null);
        
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) return new Response<>(MessageType.ERROR, false, "Unauthorized", null);

        try {
            boolean success = userService.updateAvatar(currentUser.getAccountname(), avatarPath);
            
            if (success) {
                currentUser.setAvt(avatarPath); // Update in-memory session
                FileLogger.info("User " + currentUser.getAccountname() + " updated their avatar.");
                return new Response<>(MessageType.SUCCESS, true, "Avatar updated successfully", null);
            } else {
                return new Response<>(MessageType.ERROR, false, "Failed to update avatar", null);
            }
        } catch (Exception e) {
            FileLogger.error("Error updating avatar", e);
            return new Response<>(MessageType.ERROR, false, "Internal server error", null);
        }
    }
}
