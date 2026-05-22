package org.example.server.network.command;

import org.example.dto.LoginRequest;
import org.example.dto.UserResponse;
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
        
        Object result = authController.authenticateAndGetUser(loginReq);
        
        if (result instanceof User user) {
            SessionManager.login(channel, user);
            return new Response<>(MessageType.SUCCESS, true, "Login successful", new UserResponse(user));
        } else if (result instanceof Response<?> errorResponse) {
            return errorResponse;
        }
        
        return new Response<>(MessageType.ERROR, false, "Invalid login credentials", null);
    }
}
