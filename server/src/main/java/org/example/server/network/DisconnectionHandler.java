package org.example.server.network;

import org.example.model.user.User;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.ProductDao;
import org.example.util.FileLogger;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * SOLID: Single Responsibility - Handles the business logic of a client disconnection.
 */
public class DisconnectionHandler {

    public static void handle(SocketChannel channel) {
        User user = SessionManager.getUser(channel);
        if (user == null) {
            cleanResources(channel);
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            ProductDao productDao = new ProductDao();
            boolean isLeading = productDao.isUserLeadingAnyAuction(conn, user.getUserId());

            if (isLeading) {
                FileLogger.warn("User " + user.getUsername() + " disconnected while leading an auction. Session removed, but bid remains active.");
            } else {
                FileLogger.info("User " + user.getUsername() + " disconnected safely.");
            }

        } catch (SQLException e) {
            FileLogger.error("Error checking auction status during disconnection for user: " + user.getUsername(), e);
        } finally {
            cleanResources(channel);
        }
    }

    private static void cleanResources(SocketChannel channel) {
        try {
            Broadcaster.removeClient(channel);
            SessionManager.logout(channel);
            HeartbeatRegistry.remove(channel);
            
            if (channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            FileLogger.error("Error closing channel during cleanup", e);
        }
    }
}
