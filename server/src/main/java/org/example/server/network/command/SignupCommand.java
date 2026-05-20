package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AuthController;

import java.nio.channels.SocketChannel;

public class SignupCommand implements Command {
    private final AuthController authController;

    public SignupCommand(AuthController authController) {
        this.authController = authController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        return authController.handleSignup(request.getPayload());
    }
}
