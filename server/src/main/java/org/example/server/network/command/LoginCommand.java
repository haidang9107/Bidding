package org.example.server.network.command;

import org.example.dto.UserResponse;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AuthController;
import org.example.server.network.SessionManager;

import java.nio.channels.SocketChannel;

public class LoginCommand implements Command {
    private final AuthController authController;

    public LoginCommand(AuthController authController) {
        this.authController = authController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        // We need the raw User object for the session, but the controller returns a Response with UserResponse
        // Let's refactor: Controller should return the User, and we wrap it here.
        // For now, let's fix it by letting AuthController return User and Command wrap it.
        Object result = authController.authenticateAndGetUser(request.getPayload());
        if (result instanceof User user) {
            SessionManager.login(channel, user);
            return new Response<>(MessageType.SUCCESS, true, "Login successful", new UserResponse(user));
        } else if (result instanceof Response<?> errorResponse) {
            return errorResponse;
        }
        return new Response<>(MessageType.ERROR, false, "Invalid login credentials", null);
    }
}
