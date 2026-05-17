package org.example.client;

import org.example.client.network.SocketClient;
import org.example.payload.MessageType;
import org.example.payload.Request;
import org.example.util.FileLogger;

/**
 * Enhanced Entry point for the Bidding Client application.
 * Demonstrates Observer pattern and new data structures.
 */
public class ClientApp {

    public static void main(String[] args) {
        try {
            SocketClient client = SocketClient.getInstance();

            // Register an observer to handle server responses
            client.addObserver(response -> {
                System.out.println("\n>>> [SERVER RESPONSE]");
                System.out.println("Type: " + response.getType());
                System.out.println("Success: " + response.isSuccess());
                System.out.println("Message: " + response.getMessage());
                if (response.getData() != null) {
                    System.out.println("Data: " + response.getData());
                }
                System.out.println("<<<");
            });

            // Connect to server
            client.connect();

            // 1. Test Login (MEMBER)
            System.out.println("Testing Login...");
            client.sendRequest(new Request(MessageType.LOGIN, "nam_member:password123"));

            Thread.sleep(1000);

            // 2. Test Get All Auctions
            System.out.println("Testing Get All Auctions...");
            client.sendRequest(new Request(MessageType.PRODUCT_LIST, null));

            Thread.sleep(1000);

            // 3. Test Place Bid (productId: 1, bidderId: 2, amount: 26,000,000 VND)
            // Note: nam_member has ID 2 in 2data.sql
            System.out.println("Testing Place Bid...");
            client.sendRequest(new Request(MessageType.BID_PLACE, "1:2:26000000"));

            // Keep alive to see broadcast notifications
            System.out.println("Waiting for potential broadcasts (real-time updates)...");
            Thread.sleep(10000);

            client.disconnect();

        } catch (Exception e) {
            FileLogger.error("ClientApp crashed", e);
        }
    }
}
