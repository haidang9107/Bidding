package org.example.server.network;

import org.example.model.user.User;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active user sessions for socket connections.
 */
public class SessionManager {
    private static final Map<SocketChannel, User> activeSessions = new ConcurrentHashMap<>();

    public static void login(SocketChannel channel, User user) {
        activeSessions.put(channel, user);
    }

    public static void logout(SocketChannel channel) {
        activeSessions.remove(channel);
    }

    public static User getUser(SocketChannel channel) {
        return activeSessions.get(channel);
    }

    public static boolean isLoggedIn(SocketChannel channel) {
        return activeSessions.containsKey(channel);
    }

    public static int getActiveSessionsCount() {
        return activeSessions.size();
    }
}
