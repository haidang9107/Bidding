package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.network.SessionManager;

import java.nio.channels.SocketChannel;

public class LogoutCommand implements Command {
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        SessionManager.logout(channel);
        return new Response<>(MessageType.SUCCESS, true, "Logout successful", null);
    }
}
