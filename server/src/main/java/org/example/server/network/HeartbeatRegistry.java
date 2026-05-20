package org.example.server.network;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SOLID: Single Responsibility - Only tracks last active timestamps for channels.
 */
public class HeartbeatRegistry {
    private static final Map<SocketChannel, Long> lastActiveTimes = new ConcurrentHashMap<>();

    public static void update(SocketChannel channel) {
        lastActiveTimes.put(channel, System.currentTimeMillis());
    }

    public static void remove(SocketChannel channel) {
        lastActiveTimes.remove(channel);
    }

    public static Long getLastActiveTime(SocketChannel channel) {
        return lastActiveTimes.get(channel);
    }

    public static Map<SocketChannel, Long> getAll() {
        return lastActiveTimes;
    }
}
