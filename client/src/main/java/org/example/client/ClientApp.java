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
 * Updated for the 'Major Surgery' (accountname as PK).
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

            // --- SCENARIO 0: System Connection ---
            testHeader("SCENARIO 0: System Connection & Heartbeat");
            System.out.println("-> Sending PING...");
            client.sendRequest(new Request(MessageType.PING, null));
            waitForResponse();

            // --- SCENARIO 1: Cleanup & Setup (Admin) ---
            testHeader("SCENARIO 1: Administrative Setup");
            System.out.println("-> Logging in as Admin to reset test state...");
            client.sendRequest(new Request(MessageType.LOGIN, new LoginRequest("admin_minh", "password123")));
            waitForResponse();
            
            System.out.println("-> Ensuring 'huong_member' is ACTIVE...");
            client.sendRequest(new Request(MessageType.ADMIN_BAN_USER, "huong_member:0"));
            waitForResponse();
            
            client.sendRequest(new Request(MessageType.LOGOUT, null));
            waitForResponse();

            // --- SCENARIO 2: Signup Test ---
            testHeader("SCENARIO 2: User Registration");
            String testUser = "new_user_" + System.currentTimeMillis() / 1000;
            System.out.println("-> Registering a new user: " + testUser);
            client.sendRequest(new Request(MessageType.SIGNUP, new SignupRequest(testUser, "password123", testUser + "@test.com")));
            waitForResponse();

            // --- SCENARIO 3: Unauthorized Access ---
            testHeader("SCENARIO 3: Accessing protected resource without Login");
            System.out.println("-> Trying to fetch product list without login...");
            client.sendRequest(new Request(MessageType.PRODUCT_LIST, null));
            waitForResponse();

            // --- SCENARIO 4: Authentication ---
            testHeader("SCENARIO 4: Authentication Tests");
            
            System.out.println("-> Testing Login with WRONG password...");
            client.sendRequest(new Request(MessageType.LOGIN, new LoginRequest("nam_member", "wrongpass")));
            waitForResponse();

            System.out.println("-> Testing Successful Login (Member: nam_member)...");
            client.sendRequest(new Request(MessageType.LOGIN, new LoginRequest("nam_member", "password123")));
            waitForResponse();

            // --- SCENARIO 5: Product Discovery ---
            testHeader("SCENARIO 5: Product Discovery");
            System.out.println("-> Fetching all active auctions...");
            client.sendRequest(new Request(MessageType.PRODUCT_LIST, null));
            waitForResponse();

            // --- SCENARIO 6: Profile Features ---
            testHeader("SCENARIO 6: Profile Features");
            System.out.println("-> Updating Avatar for 'nam_member'...");
            client.sendRequest(new Request(MessageType.USER_UPDATE_AVATAR, "https://api.dicebear.com/7.x/avataaars/svg?seed=nam"));
            waitForResponse();

            // --- SCENARIO 7: Authorization & RBAC ---
            testHeader("SCENARIO 7: Role-Based Access Control (RBAC)");
            
            System.out.println("-> Member trying to ADD_PRODUCT (Should be FORBIDDEN)...");
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

            // --- SCENARIO 8: Financial Operations & Bidding ---
            testHeader("SCENARIO 8: Financial & Bidding Operations");
            
            System.out.println("-> Switching to Member (Huong) for testing...");
            client.sendRequest(new Request(MessageType.LOGOUT, null));
            waitForResponse();
            client.sendRequest(new Request(MessageType.LOGIN, new LoginRequest("huong_member", "password123")));
            waitForResponse();

            System.out.println("-> Testing DEPOSIT (Amount: 10,000,000) to prepare for bid...");
            client.sendRequest(new Request(MessageType.DEPOSIT, 10000000L));
            waitForResponse();

            System.out.println("-> Placing a valid bid on Product ID: 1...");
            // Min bid is 25,500,000 if current is 25,000,000. Huong now has ~30,000,000.
            client.sendRequest(new Request(MessageType.BID_PLACE, new BidRequest(1, "huong_member", 26000000L)));
            waitForResponse();

            System.out.println("-> Testing TRANSFER (To 'nam_member', Amount: 1,000,000)...");
            client.sendRequest(new Request(MessageType.TRANSFER, new TransferRequest("nam_member", 1000000L)));
            waitForResponse();

            System.out.println("-> Testing WITHDRAW (Amount: 500,000)...");
            client.sendRequest(new Request(MessageType.WITHDRAW, 500000L));
            waitForResponse();

            // --- SCENARIO 9: Admin Controls (Banning) ---
            testHeader("SCENARIO 9: Admin Controls");
            System.out.println("-> Switching back to Admin to ban 'huong_member'...");
            client.sendRequest(new Request(MessageType.LOGOUT, null));
            waitForResponse();
            client.sendRequest(new Request(MessageType.LOGIN, new LoginRequest("admin_minh", "password123")));
            waitForResponse();
            
            System.out.println("-> Admin banning 'huong_member'...");
            client.sendRequest(new Request(MessageType.ADMIN_BAN_USER, "huong_member:1"));
            waitForResponse();

            System.out.println("-> Banned user 'huong_member' trying to login (Should FAIL)...");
            client.sendRequest(new Request(MessageType.LOGOUT, null));
            waitForResponse();
            client.sendRequest(new Request(MessageType.LOGIN, new LoginRequest("huong_member", "password123")));
            waitForResponse();

            // --- SCENARIO 11: Cleanup ---
            testHeader("SCENARIO 11: Cleanup & Disconnect");
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
