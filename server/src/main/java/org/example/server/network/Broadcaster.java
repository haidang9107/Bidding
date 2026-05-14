package org.example.server.network;

import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class Broadcaster {
    private static final List<NotificationHandler> clients = new CopyOnWriteArrayList<>();

    public static void addClient(NotificationHandler client) {
        clients.add(client);
    }

    public static void removeClient(NotificationHandler client) {
        clients.remove(client);
    }

    public static void broadcast(Response response) {
        String jsonMessage = JsonConverter.toJson(response);
        for (NotificationHandler client : clients) {
            client.sendMessage(jsonMessage);
        }
    }
}
