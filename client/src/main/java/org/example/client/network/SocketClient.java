package org.example.client.network;

import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {
    private static SocketClient instance;
    private Socket commandSocket;
    private Socket notifySocket;
    private PrintWriter commandOut;
    private BufferedReader commandIn;
    private BufferedReader notifyIn;
    private volatile boolean connected = false;

    private SocketClient() {}

    public static SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    public void connect(String host, int port) throws IOException {
        if (connected) return;

        // Connect to Command Server
        commandSocket = new Socket(host, port);
        commandOut = new PrintWriter(commandSocket.getOutputStream(), true);
        commandIn = new BufferedReader(new InputStreamReader(commandSocket.getInputStream()));

        // Connect to Notification Server (port + 1)
        notifySocket = new Socket(host, port + 1);
        notifyIn = new BufferedReader(new InputStreamReader(notifySocket.getInputStream()));

        connected = true;
        
        // Thread for Command Responses
        new Thread(this::listenCommands, "CommandListener").start();
        
        // Thread for Notifications
        new Thread(this::listenNotifications, "NotificationListener").start();

        System.out.println(">>> Connected to server at " + host + " (Ports: " + port + ", " + (port + 1) + ")");
    }

    private void listenCommands() {
        try {
            String line;
            while (connected && (line = commandIn.readLine()) != null) {
                Response response = JsonConverter.fromJson(line, Response.class);
                handleResponse(response, "Command");
            }
        } catch (IOException e) {
            if (connected) {
                System.err.println(">>> Command connection lost: " + e.getMessage());
                disconnect();
            }
        }
    }

    private void listenNotifications() {
        try {
            String line;
            while (connected && (line = notifyIn.readLine()) != null) {
                Response response = JsonConverter.fromJson(line, Response.class);
                handleResponse(response, "Notification");
            }
        } catch (IOException e) {
            if (connected) {
                System.err.println(">>> Notification connection lost: " + e.getMessage());
                disconnect();
            }
        }
    }

    public void sendRequest(Request request) {
        if (!connected) {
            System.err.println(">>> Not connected to server!");
            return;
        }
        String json = JsonConverter.toJson(request);
        commandOut.println(json);
    }

    private void handleResponse(Response response, String source) {
        System.out.println(">>> [" + source + "] Received: " + response.getMessage());
        // Here you could use a callback or EventBus to notify the UI
    }

    public void disconnect() {
        connected = false;
        try {
            if (commandSocket != null) commandSocket.close();
            if (notifySocket != null) notifySocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
