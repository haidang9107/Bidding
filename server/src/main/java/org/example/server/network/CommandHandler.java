package org.example.server.network;

import org.example.model.user.User;
import org.example.payload.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AuthController;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.UserDao;
import org.example.server.service.user.auth.AuthService;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Handles a single command request from a client SocketChannel.
 */
public class CommandHandler implements Runnable {
    private final SocketChannel clientChannel;
    private final String message;

    public CommandHandler(SocketChannel clientChannel, String message) {
        this.clientChannel = clientChannel;
        this.message = message;
    }

    @Override
    public void run() {
        try {
            Request request = JsonConverter.fromJson(message, Request.class);
            Response<?> response = handleRequest(request);
            sendResponse(response);
        } catch (Exception e) {
            FileLogger.error("Error handling command", e);
            sendResponse(new Response<>(MessageType.ERROR, false, "Internal Server Error: " + e.getMessage(), null));
        }
    }

    private Response<?> handleRequest(Request request) {
        try (Connection conn = DatabaseManager.getConnection()) {
            UserDao userDao = new UserDao(conn);
            AuthService authService = new AuthService(userDao);
            AuthController authController = new AuthController(authService);

            switch (request.getType()) {
                case LOGIN:
                    return authController.handleLogin(request.getPayload());

                case SIGNUP:
                    return authController.handleSignup(request.getPayload());

                default:
                    return new Response<>(MessageType.ERROR, false, "Unsupported command type: " + request.getType(), null);
            }
        } catch (SQLException e) {
            FileLogger.error("Database error during request handling", e);
            return new Response<>(MessageType.ERROR, false, "Database Error", null);
        }
    }

    private void sendResponse(Response<?> response) {
        try {
            String json = JsonConverter.toJson(response) + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                clientChannel.write(buffer);
            }
        } catch (IOException e) {
            FileLogger.error("Failed to send response to client", e);
        }
    }
}
