package org.example.server.controller;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Response;
import org.example.server.network.SessionManager;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.nio.channels.SocketChannel;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Controller for handling common user-related requests (non-auth).
 */
public class UserController {
    private final UserDao userDao;

    public UserController() {
        this.userDao = new UserDao();
    }

    /**
     * Updates the avatar of the logged-in user.
     */
    public Response<String> handleUpdateAvatar(Object payload, SocketChannel channel) {
        if (payload == null) return new Response<>(MessageType.ERROR, false, "Avatar path required", null);
        
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) return new Response<>(MessageType.ERROR, false, "Unauthorized", null);

        try (Connection conn = DatabaseManager.getConnection()) {
            String avatarPath = payload.toString();
            boolean success = userDao.updateAvatar(conn, currentUser.getAccountname(), avatarPath);
            
            if (success) {
                currentUser.setAvt(avatarPath); // Update in-memory session
                FileLogger.info("User " + currentUser.getAccountname() + " updated their avatar.");
                return new Response<>(MessageType.SUCCESS, true, "Avatar updated successfully", null);
            } else {
                return new Response<>(MessageType.ERROR, false, "Failed to update avatar in DB", null);
            }
        } catch (SQLException e) {
            FileLogger.error("Error updating avatar", e);
            return new Response<>(MessageType.ERROR, false, "Internal DB error", null);
        }
    }
}
