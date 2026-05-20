package org.example.server.network.command;

import org.example.payload.Request;
import org.example.payload.Response;
import java.nio.channels.SocketChannel;

/**
 * SOLID: Interface Segregation & Open/Closed.
 * Base interface for all executable commands in the system.
 */
public interface Command {
    Response<?> execute(Request request, SocketChannel channel);
}
