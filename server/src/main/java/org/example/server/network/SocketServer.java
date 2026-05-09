package org.example.server.network;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class SocketServer implements CommandLineRunner {
    private static final int PORT = 8888;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private boolean running = true;

    @Override
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
        this.running = false;
    }
}
