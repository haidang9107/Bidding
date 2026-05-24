package org.example.server.network;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SOLID: Single Responsibility - Only tracks last active timestamps for channels.
 */
public class HeartbeatRegistry {
    private static final Map<SocketChannel, Long> lastActiveTimes = new ConcurrentHashMap<>();

    /**
     * Updates the last active timestamp for a channel.
     * @param channel The socket channel.
     */
    public static void update(SocketChannel channel) {
        lastActiveTimes.put(channel, System.currentTimeMillis());
    }

    /**
     * Removes a channel from the registry.
     * @param channel The socket channel.
     */
    public static void remove(SocketChannel channel) {
        lastActiveTimes.remove(channel);
    }

    /**
     * Retrieves the last active timestamp for a channel.
     * @param channel The socket channel.
     * @return The timestamp in milliseconds.
     */
    public static Long getLastActiveTime(SocketChannel channel) {
        return lastActiveTimes.get(channel);
    }

    /**
     * Returns a map of all registered channels and their last active times.
     * @return The map.
     */
    public static Map<SocketChannel, Long> getAll() {
        return lastActiveTimes;
    }
}
