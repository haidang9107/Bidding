package org.example.server.network.command;

import org.example.payload.Request;
import org.example.payload.Response;
import java.nio.channels.SocketChannel;

/**
 * SOLID: Interface Segregation & Open/Closed.
 * Base interface for all executable commands in the system.
 */
public interface Command {
    /**
     * Executes the command logic.
     *
     * @param request the request from the client containing the payload
     * @param channel the socket channel associated with the client session
     * @return the response to be sent back to the client
     */
    Response<?> execute(Request request, SocketChannel channel);
}
