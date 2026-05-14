package org.example.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer {
    private static final int CMD_PORT = 8888;
    private static final int NOTIFY_PORT = 8889;
    private volatile boolean running = true;

    public void run(String... args) {
        new Thread(() -> startServer(CMD_PORT, "Command")).start();
        new Thread(() -> startServer(NOTIFY_PORT, "Notification")).start();
    }

    private void startServer(int port, String type) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(">>> " + type + " Server is LIVE on port: " + port);
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(">>> New " + type + " connection: " + clientSocket.getRemoteSocketAddress());
                
                if (type.equals("Command")) {
                    new Thread(new CommandHandler(clientSocket)).start();
                } else {
                    NotificationHandler handler = new NotificationHandler(clientSocket);
                    Broadcaster.addClient(handler);
                    // We need a way to detect client disconnection for Notification sockets
                    // Since it's only for sending, we might need a heartbeat or check on write
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println(">>> " + type + " Server Error: " + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        // In a real app, we'd close the ServerSockets here
    }
}
