package org.example.client.network;

import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Singleton class managing the socket connection between the client and the server.
 * It handles both command-based requests and real-time notifications.
 */
public class SocketClient {
    private static volatile SocketClient instance;
    private Socket commandSocket;
    private Socket notifySocket;
    private PrintWriter commandOut;
    private BufferedReader commandIn;
    private BufferedReader notifyIn;
    private volatile boolean connected = false;

    private SocketClient() {}

    /**
     * Returns the singleton instance of SocketClient.
     * @return The SocketClient instance.
     */
    public static SocketClient getInstance() {
        if (instance == null) {
            synchronized (SocketClient.class) {
                if (instance == null) {
                    instance = new SocketClient();
                }
            }
        }
        return instance;
    }

    /**
     * Establishes a connection to the server's command and notification ports.
     * @param host The server hostname or IP address.
     * @param port The base port (command port). The notification port is assumed to be port + 1.
     * @throws IOException If a connection error occurs.
     */
    public void connect(String host, int port) throws IOException {
        if (connected) return;

        try {
            // Connect to Command Server
            commandSocket = new Socket(host, port);
            commandOut = new PrintWriter(commandSocket.getOutputStream(), true);
            commandIn = new BufferedReader(new InputStreamReader(commandSocket.getInputStream()));

            // Connect to Notification Server (port + 1)
            notifySocket = new Socket(host, port + 1);
            notifyIn = new BufferedReader(new InputStreamReader(notifySocket.getInputStream()));

            connected = true;
            FileLogger.info("Connected to server at " + host + " (Ports: " + port + ", " + (port + 1) + ")");
            
            // Thread for Command Responses
            new Thread(this::listenCommands, "CommandListener").start();
            
            // Thread for Notifications
            new Thread(this::listenNotifications, "NotificationListener").start();
        } catch (IOException e) {
            FileLogger.error("Failed to connect to server: " + host, e);
            throw e;
        }
    }

    /**
     * Listens for incoming responses from the command server in a background thread.
     */
    private void listenCommands() {
        try {
            String line;
            while (connected && (line = commandIn.readLine()) != null) {
                Response response = JsonConverter.fromJson(line, Response.class);
                FileLogger.info("[Command] Received: " + response.getMessage());
                handleResponse(response, "Command");
            }
        } catch (IOException e) {
            if (connected) {
                FileLogger.error("Command connection lost", e);
                disconnect();
            }
        }
    }

    /**
     * Listens for incoming real-time notifications from the server in a background thread.
     */
    private void listenNotifications() {
        try {
            String line;
            while (connected && (line = notifyIn.readLine()) != null) {
                Response response = JsonConverter.fromJson(line, Response.class);
                FileLogger.info("[Notification] Received: " + response.getMessage());
                handleResponse(response, "Notification");
            }
        } catch (IOException e) {
            if (connected) {
                FileLogger.error("Notification connection lost", e);
                disconnect();
            }
        }
    }

    /**
     * Sends a request to the server via the command socket.
     * @param request The request object to send.
     */
    public void sendRequest(Request request) {
        if (!connected) {
            FileLogger.error("Cannot send request: Not connected to server");
            return;
        }
        String json = JsonConverter.toJson(request);
        commandOut.println(json);
    }

    /**
     * Handles responses received from the server.
     * @param response The response object.
     * @param source The source of the response ("Command" or "Notification").
     */
    private void handleResponse(Response response, String source) {
        // Implementation for UI updates or event handling goes here
    }

    /**
     * Disconnects the client from the server and closes all sockets.
     */
    public void disconnect() {
        if (!connected) return;
        connected = false;
        try {
            if (commandSocket != null) commandSocket.close();
            if (notifySocket != null) notifySocket.close();
            FileLogger.info("Disconnected from server.");
        } catch (IOException e) {
            FileLogger.error("Error during disconnect", e);
        }
    }

    /**
     * Checks if the client is currently connected to the server.
     * @return true if connected, false otherwise.
     */
    public boolean isConnected() {
        return connected;
    }
}
