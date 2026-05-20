package org.example.client;

import org.example.client.network.SocketClient;
import org.example.dto.BidRequest;
import org.example.dto.LoginRequest;
import org.example.dto.SignupRequest;
import org.example.dto.TransferRequest;
import org.example.dto.ProductAddRequest;
import org.example.model.enums.ItemCategory;
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

            // --- SCENARIO 0: Signup Test ---
            testHeader("SCENARIO 0: User Registration");
            String testUser = "new_user_" + System.currentTimeMillis() / 1000;
            System.out.println("-> Registering a new user: " + testUser);
            client.sendRequest(new Request(MessageType.SIGNUP, new SignupRequest(testUser, "password123", testUser + "@test.com")));
            waitForResponse();

            // --- SCENARIO 1: Unauthorized Access ---
            testHeader("SCENARIO 1: Accessing protected resource without Login");
            client.sendRequest(new Request(MessageType.PRODUCT_LIST, null));
            waitForResponse();

            // --- SCENARIO 2: Authentication (Login/Signup) ---
            testHeader("SCENARIO 2: Authentication Tests");
            
            System.out.println("-> Testing Login with WRONG password...");
            client.sendRequest(new Request(MessageType.LOGIN, new LoginRequest("nam_member", "wrongpass")));
            waitForResponse();

            System.out.println("-> Testing Successful Login (Member)...");
            client.sendRequest(new Request(MessageType.LOGIN, new LoginRequest("nam_member", "password123")));
            waitForResponse();

            // --- SCENARIO 3: Authorization & RBAC ---
            testHeader("SCENARIO 3: Role-Based Access Control (RBAC)");
            
            System.out.println("-> Member trying to ADD_PRODUCT (Should be FORBIDDEN by RBAC logic in CommandHandler)...");
            ProductAddRequest memberAdd = new ProductAddRequest("Member Laptop", "Attempt by member", 1000, ItemCategory.ELECTRONICS);
            client.sendRequest(new Request(MessageType.PRODUCT_ADD, memberAdd));
            waitForResponse();

            System.out.println("-> Switching to Admin account...");
            client.sendRequest(new Request(MessageType.LOGOUT, null));
            waitForResponse();
            client.sendRequest(new Request(MessageType.LOGIN, new LoginRequest("admin_minh", "password123")));
            waitForResponse();

            System.out.println("-> Admin trying to ADD_PRODUCT (Should be SUCCESS)...");
            ProductAddRequest adminAdd = new ProductAddRequest("Admin MacBook", "Authorized add", 5000, ItemCategory.ELECTRONICS);
            client.sendRequest(new Request(MessageType.PRODUCT_ADD, adminAdd));
            waitForResponse();

            // --- SCENARIO 4: Bidding Logic ---
            testHeader("SCENARIO 4: Bidding Scenarios");
            
            System.out.println("-> Switching back to Member (Huong)...");
            client.sendRequest(new Request(MessageType.LOGIN, new LoginRequest("huong_member", "password123")));
            waitForResponse();

            System.out.println("-> Placing a valid bid on iPhone 15 (Product ID: 1)...");
            // Use BidRequest DTO
            client.sendRequest(new Request(MessageType.BID_PLACE, new BidRequest(1, 4, 27000000L)));
            waitForResponse();

            System.out.println("-> Placing an INVALID bid (Lower than current price)...");
            client.sendRequest(new Request(MessageType.BID_PLACE, new BidRequest(1, 4, 1000000L)));
            waitForResponse();

            // --- SCENARIO 5: Financial Operations ---
            testHeader("SCENARIO 5: Financial Operations");
            
            System.out.println("-> Testing DEPOSIT (Amount: 5,000,000)...");
            client.sendRequest(new Request(MessageType.DEPOSIT, 5000000L));
            waitForResponse();

            System.out.println("-> Testing WITHDRAW (Amount: 2,000,000)...");
            client.sendRequest(new Request(MessageType.WITHDRAW, 2000000L));
            waitForResponse();

            System.out.println("-> Testing TRANSFER (To User ID 2, Amount: 1,000,000)...");
            // Use TransferRequest DTO
            client.sendRequest(new Request(MessageType.TRANSFER, new TransferRequest(2, 1000000L)));
            waitForResponse();

            // --- SCENARIO 6: Heartbeat & Persistent Connection ---
            testHeader("SCENARIO 6: Heartbeat & Inactivity Tests");
            System.out.println("-> Sending PING to server (Updates lastActiveTime)...");
            client.sendRequest(new Request(MessageType.PING, null));
            waitForResponse();
            
            System.out.println("-> Sending PRODUCT_LIST (Should also update lastActiveTime via new CommandHandler logic)...");
            client.sendRequest(new Request(MessageType.PRODUCT_LIST, null));
            waitForResponse();

            // --- SCENARIO 7: Real-time Updates & Cleanup ---
            testHeader("SCENARIO 7: Cleanup & Disconnect");
            System.out.println("Waiting 2 seconds before disconnecting...");
            Thread.sleep(2000);

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
