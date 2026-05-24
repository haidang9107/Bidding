package org.example.server.network.command;

import org.example.model.enums.MessageType;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for mapping MessageTypes to their respective Command implementations.
 * This class follows the Command Pattern to decouple request handling from the network layer.
 */
public class CommandRegistry {
    private final Map<MessageType, Command> commands = new HashMap<>();

    /**
     * Registers a command for a specific message type.
     *
     * @param type the type of message this command handles
     * @param command the command implementation
     */
    public void register(MessageType type, Command command) {
        commands.put(type, command);
    }

    /**
     * Retrieves the command associated with the given message type.
     *
     * @param type the type of message
     * @return the command implementation, or null if not registered
     */
    public Command get(MessageType type) {
        return commands.get(type);
    }
}
