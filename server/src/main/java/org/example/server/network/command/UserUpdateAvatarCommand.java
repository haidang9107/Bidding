package org.example.server.network.command;

import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.UserController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for a user to update their profile avatar.
 */
public class UserUpdateAvatarCommand implements Command {
    private final UserController userController;

    /**
     * Constructs a UserUpdateAvatarCommand with the specified UserController.
     *
     * @param userController the controller for user-related operations
     */
    public UserUpdateAvatarCommand(UserController userController) {
        this.userController = userController;
    }

    /**
     * Executes the avatar update command.
     *
     * @param request the request containing the new avatar path
     * @param channel the socket channel of the user
     * @return the response indicating success or failure of the update
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        String avatarPath = JsonConverter.convert(request.getPayload(), String.class);
        return userController.handleUpdateAvatar(avatarPath, channel);
    }
}
