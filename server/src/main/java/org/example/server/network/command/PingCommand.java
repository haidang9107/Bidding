package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.network.HeartbeatRegistry;

import java.nio.channels.SocketChannel;

/**
 * Command to handle heartbeat (ping) from the client to maintain the connection.
 */
public class PingCommand implements Command {
    /**
     * Executes the ping command and updates the heartbeat registry.
     *
     * @param request the request from the client
     * @param channel the socket channel of the client
     * @return the PONG response
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        HeartbeatRegistry.update(channel);
        return new Response<>(MessageType.PONG, true, "PONG", null);
    }
}
