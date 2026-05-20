package org.example.server.network;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AuthController;
import org.example.server.controller.BidController;
import org.example.server.controller.ProductController;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.UserDao;
import org.example.server.repository.ProductDao;
import org.example.server.service.user.auth.AuthService;
import org.example.server.service.product.ProductService;
import org.example.server.service.bid.BidService;
import org.example.server.network.command.Command;
import org.example.server.network.command.CommandRegistry;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * SOLID: Single Responsibility - Handles network request processing and command execution.
 */
public class CommandHandler implements Runnable {
    private final SocketChannel clientChannel;
    private final String message;
    private final CommandRegistry commandRegistry;

    public CommandHandler(SocketChannel clientChannel, String message, CommandRegistry commandRegistry) {
        this.clientChannel = clientChannel;
        this.message = message;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public void run() {
        try {
            // Heartbeat: Update every time a client sends a valid message
            HeartbeatRegistry.update(clientChannel);

            Request request = JsonConverter.fromJson(message, Request.class);
            if (request == null) return;
            
            Response<?> response = handleRequest(request);
            sendResponse(response);
            
            // Notification Broadcast for Bids
            if (request.getType() == MessageType.BID_PLACE && response.isSuccess()) {
                // In a real scenario, we'd extract the productId from the payload properly
                FileLogger.info("Bid placed successfully, triggering broadcast logic (to be refined with DTO)");
            }
            
        } catch (Exception e) {
            FileLogger.error("Error handling command: " + message, e);
            sendResponse(new Response<>(MessageType.ERROR, false, "Internal Server Error", null));
        }
    }

    private Response<?> handleRequest(Request request) {
        User currentUser = SessionManager.getUser(clientChannel);

        // 1. Authentication Check
        if (request.getType() != MessageType.LOGIN && request.getType() != MessageType.SIGNUP && request.getType() != MessageType.PING) {
            if (currentUser == null) {
                return new Response<>(MessageType.ERROR, false, "Unauthorized: Please login first", null);
            }
        }

        // 2. Authorization Check (RBAC)
        if (!hasPermission(request.getType(), currentUser)) {
            return new Response<>(MessageType.ERROR, false, "Forbidden: You don't have permission for " + request.getType(), null);
        }

        // 3. Command Execution (SOLID: Command Pattern)
        Command command = commandRegistry.get(request.getType());
        if (command == null) {
            return new Response<>(MessageType.ERROR, false, "Unknown Command: " + request.getType(), null);
        }

        return command.execute(request, clientChannel);
    }

    /**
     * Helper to verify if a user has permission for a specific message type.
     */
    private boolean hasPermission(MessageType type, User user) {
        // Public routes
        if (type == MessageType.LOGIN || type == MessageType.SIGNUP || type == MessageType.PING) return true;
        
        if (user == null) return false;

        // RBAC Logic
        return switch (type) {
            case PRODUCT_ADD -> user.getRole() == org.example.model.enums.UserRole.ADMIN;
            // Add more specific role checks here
            default -> true; 
        };
    }

    private void broadcastBidUpdate(int productId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            ProductDao productDao = new ProductDao();
            Object product = productDao.getProductById(conn, productId);
            Response<Object> update = new Response<>(MessageType.BID_UPDATE, true, "New highest bid!", product);
            Broadcaster.broadcast(update);
        } catch (SQLException e) {
            FileLogger.error("Failed to broadcast bid update", e);
        }
    }

    private void sendResponse(Response<?> response) {
        synchronized (clientChannel) {
            try {
                String json = JsonConverter.toJson(response) + "\n";
                ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
                while (buffer.hasRemaining()) {
                    int written = clientChannel.write(buffer);
                    if (written == 0) {
                        Thread.onSpinWait();
                    }
                }
            } catch (IOException e) {
                FileLogger.error("Failed to send response", e);
            }
        }
    }
}
