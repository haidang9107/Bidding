package org.example.server.network.command;

import org.example.dto.request.LoginRequest;
import org.example.dto.response.UserResponse;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AuthController;
import org.example.server.network.DisconnectionHandler;
import org.example.server.network.SessionManager;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * Command to handle user login and establish a session.
 */
public class LoginCommand implements Command {
    private final AuthController authController;

    /**
     * Constructs a LoginCommand with the specified AuthController.
     *
     * @param authController the controller for authentication operations
     */
    public LoginCommand(AuthController authController) {
        this.authController = authController;
    }

    /**
     * Executes the login command.
     *
     * @param request the request containing LoginRequest
     * @param channel the socket channel of the user
     * @return the response indicating success or failure of the login
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        LoginRequest loginReq = JsonConverter.convert(request.getPayload(), LoginRequest.class);
        
        User user = authController.authenticateAndGetUser(loginReq);
        
        if (user != null) {
            // Kickout Logic: Check if user is already logged in elsewhere
            SocketChannel oldChannel = SessionManager.findChannelByUsername(user.getAccountname());
            if (oldChannel != null && oldChannel != channel) {
                FileLogger.info("User " + user.getAccountname() + " logged in from another location. Kicking out old session.");
                notifyKickout(oldChannel);
                DisconnectionHandler.handle(oldChannel);
            }

            SessionManager.login(channel, user);
            return new Response<>(MessageType.SUCCESS, true, "Login successful", new UserResponse(user));
        }
        
        return new Response<>(MessageType.ERROR, false, "Invalid login credentials", null);
    }

    private void notifyKickout(SocketChannel channel) {
        try {
            Response<String> response = new Response<>(MessageType.ERROR, false, 
                "Your account has been logged in from another device. You have been disconnected.", "KICKED_OUT");
            String json = JsonConverter.toJson(response) + "\n";
            channel.write(ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            FileLogger.warn("Failed to notify kickout to channel: " + channel);
        }
    }
}
