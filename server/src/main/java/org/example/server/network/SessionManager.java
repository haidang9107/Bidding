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

    /**
     * Registers a user as logged in on a specific channel.
     * @param channel the client's socket channel
     * @param user the logged-in user
     */
    public static void login(SocketChannel channel, User user) {
        activeSessions.put(channel, user);
    }

    /**
     * Logs out a user from a specific channel.
     * @param channel the client's socket channel
     */
    public static void logout(SocketChannel channel) {
        activeSessions.remove(channel);
    }

    /**
     * Retrieves the user associated with a specific channel.
     * @param channel the client's socket channel
     * @return the user, or null if not logged in
     */
    public static User getUser(SocketChannel channel) {
        return activeSessions.get(channel);
    }

    /**
     * Checks if a user is logged in on a specific channel.
     * @param channel the client's socket channel
     * @return true if logged in, false otherwise
     */
    public static boolean isLoggedIn(SocketChannel channel) {
        return activeSessions.containsKey(channel);
    }

    /**
     * Finds the channel associated with a specific username.
     * @param username the username to search for
     * @return the socket channel, or null if not found
     */
    public static SocketChannel findChannelByUsername(String username) {
        for (Map.Entry<SocketChannel, User> entry : activeSessions.entrySet()) {
            if (entry.getValue().getAccountname().equals(username)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Gets the total number of active sessions.
     * @return the active sessions count
     */
    public static int getActiveSessionsCount() {
        return activeSessions.size();
    }
}
