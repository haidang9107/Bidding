package org.example.client;

import org.example.client.network.SocketClient;
import org.example.payload.MessageType;
import org.example.payload.Request;
import org.example.util.FileLogger;

/**
 * Entry point for the Bidding Client application.
 */
public class ClientApp {

    public static void main(String[] args) {
        try {
            SocketClient client = SocketClient.getInstance();

            // Connect using default config from .env or fallback
            client.connect();

            // Send a test Login request
            client.sendRequest(new Request(MessageType.LOGIN, "admin_minh:password123"));

            // Wait and send another one
            Thread.sleep(2000);
            client.sendRequest(new Request(MessageType.LOGIN, "admin_minh:wrong_pass"));

            // Keep alive to see responses
            Thread.sleep(5000);

            client.disconnect();

        } catch (Exception e) {
            FileLogger.error("ClientApp crashed", e);
        }
    }
}
