package org.example.client;

import org.example.client.network.SocketClient;
import org.example.payload.MessageType;
import org.example.payload.Request;

public class ClientApp {
    public static void main(String[] args) {
        try {
            // 1. Get the singleton instance
            SocketClient client = SocketClient.getInstance();

            // 2. Connect to server
            client.connect("localhost", 8888);

            // 3. Send a test Login request
            System.out.println(">>> Sending Login Request...");
            client.sendRequest(new Request(MessageType.LOGIN, "Test User Data"));

            // 4. Keep the main thread alive for a few seconds to see the response
            Thread.sleep(3000);

            // 5. Disconnect
            client.disconnect();
            System.out.println(">>> Test Finished.");

        } catch (Exception e) {
            System.err.println(">>> Client Error: " + e.getMessage());
        }
    }
}
