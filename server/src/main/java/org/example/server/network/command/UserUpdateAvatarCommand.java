package org.example.server.network.command;

import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.UserController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

public class UserUpdateAvatarCommand implements Command {
    private final UserController userController;

    public UserUpdateAvatarCommand(UserController userController) {
        this.userController = userController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        String avatarPath = JsonConverter.convert(request.getPayload(), String.class);
        return userController.handleUpdateAvatar(avatarPath, channel);
    }
}
