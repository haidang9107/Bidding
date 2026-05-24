package org.example.server.network;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.ErrorDetail;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.exception.BaseAppException;
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
    private final org.example.server.service.user.auth.AuthService authService;

    public CommandHandler(SocketChannel clientChannel, String message, CommandRegistry commandRegistry, org.example.server.service.user.auth.AuthService authService) {
        this.clientChannel = clientChannel;
        this.message = message;
        this.commandRegistry = commandRegistry;
        this.authService = authService;
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
            
        } catch (BaseAppException e) {
            FileLogger.warn("Application error handling command: " + e.getMessage() + " [" + e.getErrorCode() + "]");
            sendResponse(new Response<>(MessageType.ERROR, false, e.getMessage(), new ErrorDetail(e.getErrorCode(), e.getMessage())));
        } catch (Exception e) {
            FileLogger.error("Critical error handling command: " + JsonConverter.maskSensitiveData(message), e);
            sendResponse(new Response<>(MessageType.ERROR, false, "Internal Server Error", new ErrorDetail("INTERNAL_SERVER_ERROR", e.getMessage())));
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

        // 2. Authorization Check (RBAC) - Centralized in Service
        if (!authService.canAccess(request.getType(), currentUser)) {
            return new Response<>(MessageType.ERROR, false, "Forbidden: You don't have permission for " + request.getType(), null);
        }

        // 3. Command Execution (SOLID: Command Pattern)
        Command command = commandRegistry.get(request.getType());
        if (command == null) {
            return new Response<>(MessageType.ERROR, false, "Unknown Command: " + request.getType(), null);
        }

        return command.execute(request, clientChannel);
    }

    private void sendResponse(Response<?> response) {
        synchronized (clientChannel) {
            try {
                String json = JsonConverter.toJson(response);
                FileLogger.info("<<< OUTGOING to " + clientChannel.getRemoteAddress() + ": " + JsonConverter.maskSensitiveData(json));

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
