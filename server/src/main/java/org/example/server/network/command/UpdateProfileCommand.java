package org.example.server.network.command;

import org.example.dto.request.UserProfileUpdateRequest;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.UserController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for a user to update their profile information.
 */
public class UpdateProfileCommand implements Command {
    private final UserController userController;

    /**
     * Constructs an UpdateProfileCommand with the specified UserController.
     *
     * @param userController the controller for user-related operations
     */
    public UpdateProfileCommand(UserController userController) {
        this.userController = userController;
    }

    /**
     * Executes the profile update command.
     *
     * @param request the request containing UserProfileUpdateRequest
     * @param channel the socket channel of the user
     * @return the response indicating success or failure of the update
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        UserProfileUpdateRequest updateReq = JsonConverter.convert(request.getPayload(), UserProfileUpdateRequest.class);
        return userController.handleUpdateProfile(updateReq, channel);
    }
}
