package org.example.server.network.command;

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
        Response<?> response = authController.handleLogin(request.getPayload());
        if (response.isSuccess() && response.getData() instanceof User user) {
            SessionManager.login(channel, user);
        }
        return response;
    }
}
