package org.example.server.network;

import org.example.util.FileLogger;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Manages the socket server for both command and notification ports.
 */
public class SocketServer {
    private static final int CMD_PORT = 8888;
    private static final int NOTIFY_PORT = 8889;
    private volatile boolean running = true;

    /**
     * Starts the command and notification servers in separate threads.
     *
     * @param args optional arguments
     */
    public void run(String... args) {
        new Thread(() -> startServer(CMD_PORT, "Command")).start();
        new Thread(() -> startServer(NOTIFY_PORT, "Notification")).start();
    }

    /**
     * Internal method to start a server on a specific port.
     *
     * @param port the port to listen on
     * @param type the type of server (Command or Notification)
     */
    private void startServer(int port, String type) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            FileLogger.info(type + " Server is LIVE on port: " + port);
            while (running) {
                Socket clientSocket = serverSocket.accept();
                FileLogger.info("New " + type + " connection: " + clientSocket.getRemoteSocketAddress());
                
                if (type.equals("Command")) {
                    new Thread(new CommandHandler(clientSocket)).start();
                } else {
                    NotificationHandler handler = new NotificationHandler(clientSocket);
                    Broadcaster.addClient(handler);
                }
            }
        } catch (IOException e) {
            if (running) {
                FileLogger.error(type + " Server Error", e);
            }
        }
    }

    /**
     * Stops the server.
     */
    public void stop() {
        running = false;
    }
}
