package org.example.client;

import org.example.client.network.SocketClient;
import org.example.payload.MessageType;
import org.example.payload.Request;

/**
 * Entry point for the Bidding Client application.
 * This class handles initial connection and test requests to the server.
 */
public class ClientApp {

    /**
     * Main method to start the client application.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        try {
            // 1. Get the singleton instance
            SocketClient client = SocketClient.getInstance();

            // 2. Connect to server
            client.connect("localhost", 8888);

            // 3. Send a test Login request
            client.sendRequest(new Request(MessageType.LOGIN, "admin_minh:password123"));

            // Add a test case with wrong password to verify logic
            Thread.sleep(2000);
            client.sendRequest(new Request(MessageType.LOGIN, "admin_minh:wrong_pass"));

            // 4. Keep the main thread alive for a few seconds to see the response
            Thread.sleep(3000);

            // 5. Disconnect
            client.disconnect();

        } catch (Exception e) {
            // Error logged by SocketClient
        }
    }
}
