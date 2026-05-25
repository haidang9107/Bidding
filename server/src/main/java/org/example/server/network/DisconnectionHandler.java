package org.example.server.network;

import org.example.model.user.User;
import org.example.server.repository.AuctionDao;
import org.example.server.repository.TransactionManager;
import org.example.util.FileLogger;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * SOLID: Single Responsibility - Handles the business logic and resource cleanup of a client disconnection.
 */
public class DisconnectionHandler {

    private final TransactionManager txManager;
    private final AuctionDao auctionDao;

    public DisconnectionHandler(TransactionManager txManager) {
        this.txManager = txManager;
        this.auctionDao = new AuctionDao(); // Could be injected further, but this keeps it clean
    }

    /**
     * Executes the cleanup process when a client disconnects.
     * @param channel The socket channel that was disconnected.
     */
    public void handle(SocketChannel channel) {
        User user = SessionManager.getUser(channel);
        if (user == null) {
            cleanResources(channel);
            return;
        }

        try {
            boolean isLeading = txManager.query(conn -> 
                    auctionDao.isUserLeadingAnyAuction(conn, user.getAccountname())
            );

            if (isLeading) {
                FileLogger.warn("User " + user.getAccountname() + " disconnected while leading an auction. Session removed, but bid remains active.");
            } else {
                FileLogger.info("User " + user.getAccountname() + " disconnected safely.");
            }

        } catch (Exception e) {
            FileLogger.error("Error checking auction status during disconnection for user: " + user.getAccountname(), e);
        } finally {
            cleanResources(channel);
        }
    }

    private void cleanResources(SocketChannel channel) {
        try {
            Broadcaster.removeClient(channel);
            RoomManager.removeChannel(channel);
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
