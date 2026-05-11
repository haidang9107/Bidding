package org.example.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {
    private static final int PORT = 8888;
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    private static boolean running = true;

    public void run(String... args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println(">>> Auction Socket Server is LIVE on port: " + PORT);
            System.out.println(">>> Waiting for client requests...");

            while (running) {
                // The main thread will block here until a client connects
                Socket clientSocket = serverSocket.accept();
                System.out.println(">>> Connection accepted from: " + clientSocket.getInetAddress());

                // Process the request in a separate thread from the pool
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println(">>> Server Socket Error: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }

    public void stop() {
        running = false;
    }
}
