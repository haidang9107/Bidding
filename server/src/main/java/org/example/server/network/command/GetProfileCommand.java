package org.example.server.network.command;

import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.UserController;

import java.nio.channels.SocketChannel;

public class GetProfileCommand implements Command {
    private final UserController userController;

    public GetProfileCommand(UserController userController) {
        this.userController = userController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        return userController.handleGetProfile(channel);
    }
}
