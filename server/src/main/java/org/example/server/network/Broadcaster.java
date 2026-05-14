package org.example.server.network;

import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Manages broadcasting messages to all connected notification clients.
 */
public class Broadcaster {
    private static final List<NotificationHandler> clients = new CopyOnWriteArrayList<>();

    /**
     * Adds a client to the broadcast list.
     *
     * @param client the notification handler to add
     */
    public static void addClient(NotificationHandler client) {
        clients.add(client);
    }

    /**
     * Removes a client from the broadcast list.
     *
     * @param client the notification handler to remove
     */
    public static void removeClient(NotificationHandler client) {
        clients.remove(client);
    }

    /**
     * Broadcasts a response message to all connected clients.
     *
     * @param response the response to broadcast
     */
    public static void broadcast(Response response) {
        String jsonMessage = JsonConverter.toJson(response);
        clients.removeIf(client -> !client.sendMessage(jsonMessage));
    }
}
