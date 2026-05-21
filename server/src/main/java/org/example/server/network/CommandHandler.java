package org.example.server.network;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.network.command.Command;
import org.example.server.network.command.CommandRegistry;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;

import org.example.server.repository.DatabaseManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

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
            // Log incoming raw message
            FileLogger.info(">>> INCOMING from " + clientChannel.getRemoteAddress() + ": " + message);

            // Heartbeat: Update every time a client sends a valid message
            HeartbeatRegistry.update(clientChannel);

            Request request = JsonConverter.fromJson(message, Request.class);
            if (request == null) return;
            
            Response<?> response = handleRequest(request);
            sendResponse(response);
            
            // Notification Broadcast for Bids
            if (request.getType() == MessageType.BID_PLACE && response.isSuccess()) {
                FileLogger.info("Bid placed successfully. (Broadcasting logic is handled within the command/service if implemented)");
            }
            
        } catch (Exception e) {
            FileLogger.error("Error handling command: " + message, e);
            sendResponse(new Response<>(MessageType.ERROR, false, "Internal Server Error", null));
        }
    }

    private Response<?> handleRequest(Request request) {
        // 0. Database Health Check (For commands requiring DB access)
        if (request.getType() != MessageType.PING && !DatabaseManager.isConnected()) {
            FileLogger.error("Database connection lost! Cannot process request: " + request.getType());
            return new Response<>(MessageType.ERROR, false, "Service Unavailable: Database connection lost. Please try again later.", null);
        }

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
            case ADMIN_GET_ALL_USERS, ADMIN_BAN_USER, ADMIN_CANCEL_AUCTION -> 
                user.getRole() == org.example.model.enums.UserRole.ADMIN;
            case BID_PLACE, AUTO_BID_SET, AUTO_BID_CANCEL, PRODUCT_ADD, DEPOSIT, WITHDRAW, TRANSFER,
                 JOIN_AUCTION_ROOM, LEAVE_AUCTION_ROOM -> 
                user.getRole() == org.example.model.enums.UserRole.MEMBER;
            case USER_UPDATE_AVATAR -> true; // Both can update avatars
            default -> true; 
        };
    }

    private void sendResponse(Response<?> response) {
        synchronized (clientChannel) {
            try {
                String json = JsonConverter.toJson(response);
                FileLogger.info("<<< OUTGOING to " + clientChannel.getRemoteAddress() + ": " + json);

                String messageWithNewline = json + "\n";
                ByteBuffer buffer = ByteBuffer.wrap(messageWithNewline.getBytes(StandardCharsets.UTF_8));
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
