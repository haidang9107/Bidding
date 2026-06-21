package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.network.SessionManager;

import java.nio.channels.SocketChannel;

/**
 * Command to handle user logout and terminate the session.
 */
public class LogoutCommand implements Command {
    /**
     * Executes the logout command.
     *
     * @param request the request from the client
     * @param channel the socket channel of the user
     * @return the response indicating success of the logout
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        SessionManager.logout(channel);
        return new Response<>(MessageType.LOGOUT, true, "Logout successful", null);
    }
}
