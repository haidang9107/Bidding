package org.example.server.controller;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Response;
import org.example.server.network.SessionManager;
import org.example.server.service.user.UserService;
import org.example.util.FileLogger;

import org.example.dto.UserProfileUpdateRequest;
import org.example.dto.UserResponse;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Controller for handling common user-related requests (non-auth).
 * Refactored to delegate to UserService.
 */
public class UserController {
    private final UserService userService;

    public UserController() {
        this.userService = new UserService();
    }

    /**
     * Gets the profile of the logged-in user.
     */
    public Response<UserResponse> handleGetProfile(SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        
        return new Response<>(MessageType.SUCCESS, true, "Profile fetched successfully", new UserResponse(currentUser));
    }

    /**
     * Updates the profile of the logged-in user.
     */
    public Response<String> handleUpdateProfile(UserProfileUpdateRequest request, SocketChannel channel) {
        if (request == null) return new Response<>(MessageType.ERROR, false, "Update data required", null);
        
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) return new Response<>(MessageType.ERROR, false, "Unauthorized", null);

        try {
            boolean success = true;
            if (request.getAvt() != null) {
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
     * Updates the avatar of the logged-in user.
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
