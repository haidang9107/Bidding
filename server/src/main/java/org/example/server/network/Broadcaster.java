package org.example.server.network;

import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class Broadcaster {
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void addClient(ClientHandler client) {
        clients.add(client);
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public static void broadcast(Response response) {
        String jsonMessage = JsonConverter.toJson(response);
        for (ClientHandler client : clients) {
            client.sendMessage(jsonMessage);
        }
    }

    public static void broadcastExcept(Response response, ClientHandler exceptClient) {
        String jsonMessage = JsonConverter.toJson(response);
        for (ClientHandler client : clients) {
            if (client != exceptClient) {
                client.sendMessage(jsonMessage);
            }
        }
    }
}
