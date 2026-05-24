package org.example.server.network.command;

import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.UserController;

import java.nio.channels.SocketChannel;

/**
 * Command to retrieve the profile information of the currently logged-in user.
 */
public class GetProfileCommand implements Command {
    private final UserController userController;

    /**
     * Constructs a GetProfileCommand with the specified UserController.
     *
     * @param userController the controller for user-related operations
     */
    public GetProfileCommand(UserController userController) {
        this.userController = userController;
    }

    /**
     * Executes the get profile command.
     *
     * @param request the request from the client
     * @param channel the socket channel of the user
     * @return the response containing user profile data
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        return userController.handleGetProfile(channel);
    }
}
