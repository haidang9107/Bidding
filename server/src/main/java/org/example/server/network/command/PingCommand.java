package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.network.HeartbeatRegistry;

import java.nio.channels.SocketChannel;

public class PingCommand implements Command {
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        HeartbeatRegistry.update(channel);
        return new Response<>(MessageType.PONG, true, "PONG", null);
    }
}
