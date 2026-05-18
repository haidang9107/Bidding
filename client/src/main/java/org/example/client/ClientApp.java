package org.example.client;

import org.example.client.network.SocketClient;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.util.FileLogger;

/**
 * Comprehensive Test Suite for the Bidding System.
 * Tests Authentication, Authorization, RBAC, and Real-time Updates.
 */
public class ClientApp {

    public static void main(String[] args) {
        try {
            SocketClient client = SocketClient.getInstance();

            // Logger to see responses in console
            client.addObserver(response -> {
                System.out.println("\n[SERVER RESPONSE] " + response.getType());
                System.out.println("  Status: " + (response.isSuccess() ? "SUCCESS" : "FAILED"));
                System.out.println("  Message: " + response.getMessage());
                if (response.getData() != null) {
                    System.out.println("  Data: " + response.getData());
                }
            });

            client.connect();

            // --- SCENARIO 1: Unauthorized Access ---
            testHeader("SCENARIO 1: Accessing protected resource without Login");
            client.sendRequest(new Request(MessageType.PRODUCT_LIST, null));
            waitForResponse();

            // --- SCENARIO 2: Authentication (Login/Signup) ---
            testHeader("SCENARIO 2: Authentication Tests");
            
            System.out.println("-> Testing Login with WRONG password...");
            client.sendRequest(new Request(MessageType.LOGIN, "nam_member:wrongpass"));
            waitForResponse();

            System.out.println("-> Testing Successful Login (Member)...");
            client.sendRequest(new Request(MessageType.LOGIN, "nam_member:password123"));
            waitForResponse();

            // --- SCENARIO 3: Authorization & RBAC ---
            testHeader("SCENARIO 3: Role-Based Access Control (RBAC)");
            
            System.out.println("-> Member trying to ADD_PRODUCT (Should be FORBIDDEN)...");
            client.sendRequest(new Request(MessageType.PRODUCT_ADD, "New Laptop:Description:1000:MEMBER"));
            waitForResponse();

            System.out.println("-> Switching to Admin account...");
            client.sendRequest(new Request(MessageType.LOGOUT, null));
            waitForResponse();
            client.sendRequest(new Request(MessageType.LOGIN, "admin_minh:password123"));
            waitForResponse();

            System.out.println("-> Admin trying to ADD_PRODUCT (Should be SUCCESS/NOT IMPLEMENTED but NOT Forbidden)...");
            client.sendRequest(new Request(MessageType.PRODUCT_ADD, "Admin Laptop:Description:5000:ADMIN"));
            waitForResponse();

            // --- SCENARIO 4: Bidding Logic ---
            testHeader("SCENARIO 4: Bidding Scenarios");
            
            System.out.println("-> Switching back to Member (Huong)...");
            client.sendRequest(new Request(MessageType.LOGIN, "huong_member:password123"));
            waitForResponse();

            System.out.println("-> Placing a valid bid on iPhone 15 (Product ID: 1)...");
            // Payload: "productId:bidderId:amount"
            client.sendRequest(new Request(MessageType.BID_PLACE, "1:4:27000000"));
            waitForResponse();

            System.out.println("-> Placing an INVALID bid (Lower than current price)...");
            client.sendRequest(new Request(MessageType.BID_PLACE, "1:4:1000000"));
            waitForResponse();

            // --- SCENARIO 5: Real-time Updates (Wait and see) ---
            testHeader("SCENARIO 5: Waiting for Real-time Broadcasts");
            System.out.println("Now, if another client places a bid, you should see a [BID_UPDATE] here...");
            System.out.println("Waiting 15 seconds before closing...");
            
            Thread.sleep(15000);

            client.disconnect();
            System.out.println("\nTest Suite Completed.");

        } catch (Exception e) {
            FileLogger.error("Test Suite crashed", e);
        }
    }

    private static void testHeader(String title) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(" " + title);
        System.out.println("=".repeat(60));
    }

    private static void waitForResponse() throws InterruptedException {
        Thread.sleep(1500); // Wait for async response from server
    }
}
