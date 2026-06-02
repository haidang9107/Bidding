package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.UserController;
import org.example.dto.request.UpdatePasswordRequest;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for updating the current user's password.
 */
public class UpdatePasswordCommand implements Command {
    private final UserController userController;

    public UpdatePasswordCommand(UserController userController) {
        this.userController = userController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        UpdatePasswordRequest updateReq = JsonConverter.convert(request.getPayload(), UpdatePasswordRequest.class);
        return userController.handleUpdatePassword(updateReq, channel);
    }
}
