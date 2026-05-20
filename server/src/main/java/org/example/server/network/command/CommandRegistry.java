package org.example.server.network.command;

import org.example.model.enums.MessageType;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for mapping MessageTypes to their respective Command implementations.
 */
public class CommandRegistry {
    private final Map<MessageType, Command> commands = new HashMap<>();

    public void register(MessageType type, Command command) {
        commands.put(type, command);
    }

    public Command get(MessageType type) {
        return commands.get(type);
    }
}
