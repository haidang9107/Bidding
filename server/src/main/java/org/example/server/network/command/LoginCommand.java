package org.example.server.network.command;

import org.example.dto.request.LoginRequest;
import org.example.dto.response.UserResponse;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AuthController;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

public class LoginCommand implements Command {
    private final AuthController authController;

    public LoginCommand(AuthController authController) {
        this.authController = authController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        LoginRequest loginReq = JsonConverter.convert(request.getPayload(), LoginRequest.class);
        
        User user = authController.authenticateAndGetUser(loginReq);
        
        if (user != null) {
            SessionManager.login(channel, user);
            return new Response<>(MessageType.SUCCESS, true, "Login successful", new UserResponse(user));
        }
        
        return new Response<>(MessageType.ERROR, false, "Invalid login credentials", null);
    }
}
