package org.example.server.network.command;

import org.example.dto.UserProfileUpdateRequest;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.UserController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

public class UpdateProfileCommand implements Command {
    private final UserController userController;

    public UpdateProfileCommand(UserController userController) {
        this.userController = userController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        UserProfileUpdateRequest updateReq = JsonConverter.convert(request.getPayload(), UserProfileUpdateRequest.class);
        return userController.handleUpdateProfile(updateReq, channel);
    }
}
