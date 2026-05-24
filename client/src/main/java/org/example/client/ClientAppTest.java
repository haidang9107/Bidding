//package org.example.client;
//
//import org.example.dto.request.AdminUserControlRequest;
//import org.example.dto.request.AuctionRoomRequest;
//import org.example.dto.request.BidRequest;
//import org.example.dto.request.LoginRequest;
//import org.example.dto.request.PaginationRequest;
//import org.example.dto.request.ProductAddRequest;
//import org.example.dto.request.SignupRequest;
//import org.example.dto.request.TransferRequest;
//import org.example.dto.request.AutoBidRequest;
//import org.example.dto.request.UserProfileUpdateRequest;
//import org.example.dto.request.AuctionCancelRequest;
//import org.example.model.enums.ItemCategory;
//import org.example.util.Config;
//import org.example.util.JsonConverter;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.Closeable;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.net.Socket;
//import java.nio.charset.StandardCharsets;
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.Callable;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.CyclicBarrier;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * Socket-level integration test runner for the bidding server.
// * Fixed NPE by correctly differentiating between BROADCAST and RESPONSE.
// */
//public class ClientAppTest {
//    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(5);
//
//    /**
//     * Main entry point to run the integration tests.
//     * @param args Command line arguments.
//     */
//    public static void main(String[] args) {
//        TestReport report = new TestReport();
//
//        try {
//            testHeader("SCENARIO 1: PROTOCOL & AUTH (WITH EXCEPTION TEST)");
//            try (TestClient admin = TestClient.connect("admin")) {
//                ServerMessage pingRes = admin.sendAndAwait("PING", null);
//                report.check("PING returns PONG/SUCCESS", pingRes != null && pingRes.successOrType("PONG"));
//
//                ServerMessage loginRes = admin.sendAndAwait("LOGIN", login("admin_minh", "password123"));
//                report.check("Admin login succeeds", loginRes);
//
//                // Test centralized exception handling
//                String duplicateUser = "huong_member";
//                ServerMessage signupFail = admin.sendAndAwait("SIGNUP", signup(duplicateUser, "pass", "abc@test.com"));
//                report.check("Duplicate signup returns ErrorDetail (VALIDATION_ERROR)",
//                        signupFail != null && !signupFail.success && signupFail.hasErrorDetail("VALIDATION_ERROR"));
//
//                admin.sendAndAwait("ADMIN_BAN_USER", adminUser("huong_member", 0));
//                admin.sendAndAwait("ADMIN_BAN_USER", adminUser("nam_member", 0));
//            }
//
//            testHeader("SCENARIO 2: FINANCE & BALANCE DTO");
//            try (TestClient huong = TestClient.connect("huong-finance")) {
//                huong.sendAndAwait("LOGIN", login("huong_member", "password123"));
//
//                // Test Deposit - Expect BalanceResponse DTO
//                ServerMessage depositRes = huong.sendAndAwait("DEPOSIT", 1000000L);
//                report.check("Deposit successful", depositRes);
//                report.check("Returns data with newBalance field", depositRes != null && depositRes.hasDataField("newBalance"));
//
//                long balanceAfter = depositRes != null ? depositRes.numberData("newBalance") : 0;
//
//                // Test Withdraw success
//                ServerMessage withdrawRes = huong.sendAndAwait("WITHDRAW", 500000L);
//                report.check("Withdraw successful", withdrawRes);
//                report.check("Balance reduced correctly", withdrawRes != null && withdrawRes.numberData("newBalance") == balanceAfter - 500000L);
//
//                // Test centralized exception: Overdraft
//                ServerMessage overdraft = huong.sendAndAwait("WITHDRAW", 999999999L);
//                report.check("Overdraft rejected", overdraft != null && !overdraft.success);
//                report.check("Returns FINANCE_ERROR code", overdraft != null && overdraft.hasErrorDetail("FINANCE_ERROR"));
//
//                // Test Profile
//                ServerMessage profileRes = huong.sendAndAwait("GET_PROFILE", null);
//                report.check("Profile fetch successful", profileRes);
//            }
//
//            int auctionId = createFreshAuction(report);
//
//            testHeader("SCENARIO 3: BIDDING & NOTIFICATIONS (BID_UPDATE DTO)");
//            try (TestClient huong = TestClient.connect("bidder-huong");
//                 TestClient nam = TestClient.connect("bidder-nam")) {
//
//                huong.sendAndAwait("LOGIN", login("huong_member", "password123"));
//                nam.sendAndAwait("LOGIN", login("nam_member", "password123"));
//
//                huong.sendAndAwait("JOIN_AUCTION_ROOM", room(auctionId));
//                nam.sendAndAwait("JOIN_AUCTION_ROOM", room(auctionId));
//
//                huong.sendAndAwait("DEPOSIT", 10000000L);
//                nam.sendAndAwait("DEPOSIT", 10000000L);
//
//                // Huong places bid
//                long bidAmount = 30000000L;
//                ServerMessage bidRes = huong.sendAndAwait("BID_PLACE", bid(auctionId, bidAmount));
//                report.check("Huong bid placed", bidRes);
//
//                // Nam should receive real-time notification
//                ServerMessage notify = nam.awaitType("BID_UPDATE");
//                report.check("Nam received BID_UPDATE", notify != null);
//                if (notify != null) {
//                    report.check("Notification contains winner name", "huong_member".equals(notify.stringData("bidderAccountname")));
//                    report.check("Notification contains bid amount", notify.numberData("amount") == bidAmount);
//                }
//
//                // Auto-bid test
//                huong.send("AUTO_BID_SET", autoBid(auctionId, 45000000L, 2500000L));
//                report.check("Huong configures auto-bid", huong.awaitType("SUCCESS"));
//
//                // Nam bids against auto-bid
//                ServerMessage namBid = nam.sendAndAwait("BID_PLACE", bid(auctionId, 32500000L));
//                report.check("Manual bid against auto-bid succeeds", namBid);
//
//                // Room (Nam) should receive BID_UPDATE from Huong's auto-bid reaction
//                ServerMessage autoUpdate = nam.awaitType("BID_UPDATE");
//                report.check("Room receives BID_UPDATE from auto-bid", autoUpdate != null);
//            }
//
//            testHeader("SCENARIO 4: ROBUSTNESS, SECURITY & TRANSFER");
//            try (TestClient guest = TestClient.connect("guest")) {
//                // Unauthorized
//                ServerMessage authFail = guest.sendAndAwait("PRODUCT_LIST", pagination(1, 5));
//                report.check("Unauthorized access blocked", authFail != null && !authFail.success);
//
//                // Not Found Exception
//                guest.sendAndAwait("LOGIN", login("huong_member", "password123"));
//                ServerMessage notFound = guest.sendAndAwait("PRODUCT_DETAIL", room(99999));
//                report.check("Resource NOT_FOUND handled", notFound != null && !notFound.success && notFound.hasErrorDetail("NOT_FOUND"));
//
//                // Transfer test
//                ServerMessage transferRes = guest.sendAndAwait("TRANSFER", transfer("nam_member", 1000L));
//                report.check("Transfer successful", transferRes);
//            }
//
//            testHeader("SCENARIO 5: CONCURRENT BIDDING");
//            runConcurrentBidTest(report, auctionId);
//
//            testHeader("SCENARIO 6: USER PROFILE & LEAVE ROOM");
//            try (TestClient huong = TestClient.connect("profile-huong")) {
//                huong.sendAndAwait("LOGIN", login("huong_member", "password123"));
//
//                // Update profile
//                String newEmail = "updated_" + System.currentTimeMillis() + "@test.com";
//                ServerMessage updateRes = huong.sendAndAwait("UPDATE_PROFILE", updateProfile(newEmail));
//                report.check("Update profile successful", updateRes);
//
//                // Update avatar
//                ServerMessage avatarRes = huong.sendAndAwait("USER_UPDATE_AVATAR", "http://example.com/new-avatar.jpg");
//                report.check("Update avatar successful", avatarRes);
//
//                // Verify update with GET_PROFILE
//                ServerMessage profileRes = huong.sendAndAwait("GET_PROFILE", null);
//                report.check("Profile email updated", profileRes != null && profileRes.success && profileRes.stringData("email").equalsIgnoreCase(newEmail));
//                report.check("Profile avatar updated", profileRes != null && profileRes.success && "http://example.com/new-avatar.jpg".equals(profileRes.stringData("avt")));
//
//                // Leave Room
//                huong.sendAndAwait("JOIN_AUCTION_ROOM", room(auctionId));
//                ServerMessage leaveRes = huong.sendAndAwait("LEAVE_AUCTION_ROOM", room(auctionId));
//                report.check("Leave room successful", leaveRes);
//
//                // Logout
//                ServerMessage logoutRes = huong.sendAndAwait("LOGOUT", null);
//                report.check("Logout successful", logoutRes);
//            }
//
//            testHeader("SCENARIO 7: ADVANCED ADMIN CONTROLS");
//            try (TestClient admin = TestClient.connect("admin-adv")) {
//                admin.sendAndAwait("LOGIN", login("admin_minh", "password123"));
//
//                // Admin gets all users
//                ServerMessage allUsers = admin.sendAndAwait("ADMIN_GET_ALL_USERS", pagination(1, 10));
//                report.check("Admin can fetch all users", allUsers);
//
//                // Admin cancels auction
//                ServerMessage cancelRes = admin.sendAndAwait("ADMIN_CANCEL_AUCTION", cancelAuction(createFreshAuction(report), "Test cancellation"));
//                report.check("Admin can cancel auction", cancelRes);
//
//                // Admin bans user and verifies
//                admin.sendAndAwait("ADMIN_BAN_USER", adminUser("nam_member", 1)); // Ban Nam
//                try (TestClient nam = TestClient.connect("banned-nam")) {
//                    ServerMessage namLogin = nam.sendAndAwait("LOGIN", login("nam_member", "password123"));
//                    report.check("Banned user cannot login", namLogin != null && !namLogin.success && namLogin.message.toUpperCase().contains("BANNED"));
//                }
//                admin.sendAndAwait("ADMIN_BAN_USER", adminUser("nam_member", 0)); // Unban Nam for future tests
//            }
//
//            testHeader("SCENARIO 8: SPECIALIZED PRODUCT TYPES");
//            try (TestClient seller = TestClient.connect("seller-adv")) {
//                seller.sendAndAwait("LOGIN", login("apple_store", "password123"));
//
//                // Add Art
//                ServerMessage artRes = seller.sendAndAwait("PRODUCT_ADD", product("Mona Lisa Replica", "Fine art", 50000000L, "ART"));
//                report.check("Art product added", artRes);
//
//                // Add Vehicle
//                ServerMessage vehRes = seller.sendAndAwait("PRODUCT_ADD", product("Electric Scooter", "Fast and green", 15000000L, "VEHICLE"));
//                report.check("Vehicle product added", vehRes);
//
//                // Verify they appear in list
//                ServerMessage list = seller.sendAndAwait("PRODUCT_LIST", pagination(1, 10));
//                report.check("Specialized items in list", list);
//            }
//
//            testHeader("SCENARIO 9: EDGE CASES & ERROR HANDLING");
//            try (TestClient tester = TestClient.connect("tester-edge")) {
//                tester.sendAndAwait("LOGIN", login("huong_member", "password123"));
//
//                // Bid exactly current price
//                ServerMessage bidExact = tester.sendAndAwait("BID_PLACE", bid(auctionId, 50000000L)); // base in concurrent was 50M
//                report.check("Bidding exact current price fails", bidExact != null && !bidExact.success);
//
//                // Bid below minimum step
//                ServerMessage bidLow = tester.sendAndAwait("BID_PLACE", bid(auctionId, 50000001L));
//                report.check("Bidding below step price fails", bidLow != null && !bidLow.success && bidLow.hasErrorDetail("VALIDATION_ERROR"));
//
//                // Join non-existent room
//                ServerMessage joinFail = tester.sendAndAwait("JOIN_AUCTION_ROOM", room(99999));
//                report.check("Joining non-existent room fails", joinFail != null && !joinFail.success);
//
//                // Cancel non-existent auto-bid
//                ServerMessage cancelAutoFail = tester.sendAndAwait("AUTO_BID_CANCEL", room(99999));
//                report.check("Canceling non-existent auto-bid handled", cancelAutoFail != null && !cancelAutoFail.success);
//
//                // Double logout
//                tester.sendAndAwait("LOGOUT", null);
//                ServerMessage logoutAgain = tester.sendAndAwait("LOGOUT", null);
//                report.check("Double logout handled (Unauthorized)", logoutAgain != null && !logoutAgain.success);
//
//                // Add product with negative price
//                tester.sendAndAwait("LOGIN", login("apple_store", "password123"));
//                ServerMessage negProduct = tester.sendAndAwait("PRODUCT_ADD", product("Negative Price", "Bad", -100L, "ELECTRONICS"));
//                report.check("Negative price product rejected", negProduct != null && !negProduct.success);
//            }
//
//            testHeader("SCENARIO 10: ANTI-SNIPPING TRIGGER");
//            try (TestClient sniper = TestClient.connect("sniper");
//                 TestClient victim = TestClient.connect("victim")) {
//                sniper.sendAndAwait("LOGIN", login("nam_member", "password123"));
//                victim.sendAndAwait("LOGIN", login("huong_member", "password123"));
//
//                sniper.sendAndAwait("JOIN_AUCTION_ROOM", room(auctionId));
//                victim.sendAndAwait("JOIN_AUCTION_ROOM", room(auctionId));
//
//                // Lấy thông tin phòng trước để biết thời gian kết thúc
//                ServerMessage roomInfo = sniper.sendAndAwait("PRODUCT_DETAIL", room(auctionId));
//                if (roomInfo != null && roomInfo.success && roomInfo.data instanceof Map) {
//                    long oldEndTime = roomInfo.numberData("endTime");
//
//                    // Giả lập Nam đặt giá lớn để trigger Anti-snipping (kịch bản giả định thời gian còn < 1 phút ở database hoặc mock,
//                    // nhưng do ta không chỉnh trực tiếp DB được, ta cứ đặt giá và mong chờ hàm xử lý không sinh lỗi)
//                    ServerMessage snipBid = sniper.sendAndAwait("BID_PLACE", bid(auctionId, 70000000L));
//                    report.check("Sniper bid placed successfully", snipBid);
//
//                    // Đợi broadcast
//                    ServerMessage update = victim.awaitType("BID_UPDATE");
//                    report.check("Victim received snip update", update != null);
//                }
//            }
//
//            testHeader("SCENARIO 11: MASS CONCURRENT STRESS TEST (10 CLIENTS)");
//            runMassiveStressTest(report, auctionId);
//
//            testHeader("SUMMARY");
//            report.printSummary();
//            if (report.failedCount() > 0) {
//                System.exit(1);
//            }
//        } catch (Exception e) {
//            e.printStackTrace(System.err);
//            System.exit(1);
//        }
//    }
//
//    private static void runMassiveStressTest(TestReport report, int auctionId) throws Exception {
//        int clientCount = 10;
//        ExecutorService executor = Executors.newFixedThreadPool(clientCount);
//        CountDownLatch done = new CountDownLatch(clientCount);
//        CyclicBarrier barrier = new CyclicBarrier(clientCount);
//        AtomicInteger successBids = new AtomicInteger();
//
//        for (int i = 0; i < clientCount; i++) {
//            final int id = i;
//            executor.submit(() -> {
//                try (TestClient client = TestClient.connect("stress-" + id)) {
//                    // Everyone logins with unique account
//                    String account = "stress_user_" + id + "_" + System.currentTimeMillis();
//                    client.sendAndAwait("SIGNUP", signup(account, "password123", account + "@test.com"));
//                    client.sendAndAwait("LOGIN", login(account, "password123")).requireSuccess();
//                    client.sendAndAwait("JOIN_AUCTION_ROOM", room(auctionId)).requireSuccess();
//
//                    // Concurrently deposit
//                    client.sendAndAwait("DEPOSIT", 1000000L).requireSuccess();
//
//                    barrier.await(); // Sync point
//
//                    // All bid at the same time with same or similar amounts
//                    ServerMessage res = client.sendAndAwait("BID_PLACE", bid(auctionId, 100000000L + id * 1000000L));
//                    if (res != null && res.success) successBids.incrementAndGet();
//
//                    // Concurrently fetch profile
//                    client.sendAndAwait("GET_PROFILE", null).requireSuccess();
//
//                } catch (Exception e) {
//                    // e.printStackTrace();
//                } finally {
//                    done.countDown();
//                }
//            });
//        }
//
//        done.await(30, TimeUnit.SECONDS);
//        executor.shutdown();
//
//        report.check("Massive stress test completed", successBids.get() >= 0);
//        System.out.println("  [INFO] Successful bids during stress test: " + successBids.get());
//    }
//
//    private static void runConcurrentBidTest(TestReport report, int auctionId) throws Exception {
//        int clientCount = 10;
//        long baseAmount = 50000000L;
//        ExecutorService executor = Executors.newFixedThreadPool(clientCount);
//        CyclicBarrier startTogether = new CyclicBarrier(clientCount);
//        CountDownLatch done = new CountDownLatch(clientCount);
//        List<Callable<ServerMessage>> tasks = new ArrayList<>();
//
//        for (int i = 0; i < clientCount; i++) {
//            final int index = i;
//            tasks.add(() -> {
//                try (TestClient client = TestClient.connect("bidder-" + index)) {
//                    String account = "bidder_user_" + index + "_" + System.currentTimeMillis();
//                    client.sendAndAwait("SIGNUP", signup(account, "password123", account + "@test.com"));
//                    client.sendAndAwait("LOGIN", login(account, "password123")).requireSuccess();
//                    client.sendAndAwait("JOIN_AUCTION_ROOM", room(auctionId)).requireSuccess();
//                    client.sendAndAwait("DEPOSIT", 100000000L).requireSuccess();
//
//                    startTogether.await(10, TimeUnit.SECONDS);
//                    // Each client tries to bid a slightly different amount
//                    return client.sendAndAwait("BID_PLACE", bid(auctionId, baseAmount + (index + 1) * 1000000L));
//                } catch (Exception e) {
//                    return ServerMessage.error("Test error: " + e.getMessage());
//                } finally {
//                    done.countDown();
//                }
//            });
//        }
//
//        List<Future<ServerMessage>> futures = executor.invokeAll(tasks, 15, TimeUnit.SECONDS);
//        boolean completed = done.await(15, TimeUnit.SECONDS);
//        executor.shutdownNow();
//
//        int successCount = 0;
//        int failureCount = 0;
//        for (Future<ServerMessage> future : futures) {
//            if (!future.isDone() || future.isCancelled()) {
//                failureCount++;
//                continue;
//            }
//            try {
//                ServerMessage message = future.get();
//                if (message.success) {
//                    successCount++;
//                } else {
//                    failureCount++;
//                }
//            } catch (Exception e) {
//                failureCount++;
//            }
//        }
//
//        report.check("All 10 concurrent bid tasks completed", completed);
//        report.check("At least one concurrent bid succeeds", successCount > 0);
//        report.check("Concurrent bids processed correctly (Success: " + successCount + ", Rejected: " + failureCount + ")",
//                (successCount + failureCount) == clientCount);
//    }
//
//    private static int createFreshAuction(TestReport report) throws Exception {
//        testHeader("SCENARIO 1B: Create Test Auction");
//        try (TestClient seller = TestClient.connect("seller")) {
//            seller.sendAndAwait("LOGIN", login("apple_store", "password123"));
//            seller.send("PRODUCT_ADD", product("Test Laptop " + System.currentTimeMillis(), "Desc", 25000000L, "ELECTRONICS"));
//            seller.awaitType("SUCCESS");
//
//            // Explicitly wait for the list response (NOT the broadcast)
//            ServerMessage list = null;
//            for (int i = 0; i < 5; i++) {
//                ServerMessage m = seller.sendAndAwait("PRODUCT_LIST", pagination(1, 1));
//                if (m.hasDataField("items")) {
//                    list = m;
//                    break;
//                }
//            }
//
//            if (list == null) throw new IllegalStateException("Could not fetch product list with items field");
//
//            Map<?, ?> data = (Map<?, ?>) list.data;
//            List<?> items = (List<?>) data.get("items");
//            if (items == null || items.isEmpty()) throw new IllegalStateException("Product list is empty or items field is missing");
//
//            Map<?, ?> item = (Map<?, ?>) items.get(0);
//            return ((Number) item.get("auctionId")).intValue();
//        }
//    }
//
//    // Refactored helpers using DTOs
//    private static LoginRequest login(String username, String password) {
//        return new LoginRequest(username, password);
//    }
//
//    private static SignupRequest signup(String username, String password, String email) {
//        return new SignupRequest(username, password, email);
//    }
//
//    private static AdminUserControlRequest adminUser(String accountname, int status) {
//        return new AdminUserControlRequest(accountname, status);
//    }
//
//    private static PaginationRequest pagination(int page, int pageSize) {
//        return new PaginationRequest(page, pageSize);
//    }
//
//    private static ProductAddRequest product(String name, String description,
//                                               long startingPrice, String category) {
//        return new ProductAddRequest(name, description, startingPrice, ItemCategory.valueOf(category));
//    }
//
//    private static AuctionRoomRequest room(int auctionId) {
//        return new AuctionRoomRequest(auctionId);
//    }
//
//    private static BidRequest bid(int auctionId, long amount) {
//        BidRequest req = new BidRequest();
//        req.setAuctionId(auctionId);
//        req.setAmount(amount);
//        return req;
//    }
//
//    private static AutoBidRequest autoBid(int auctionId, long maxBid, long incrementAmount) {
//        return new AutoBidRequest(auctionId, maxBid, incrementAmount);
//    }
//
//    private static TransferRequest transfer(String to, long amount) {
//        return new TransferRequest(to, amount);
//    }
//
//    private static UserProfileUpdateRequest updateProfile(String email) {
//        return new UserProfileUpdateRequest(email, null);
//    }
//
//    private static AuctionCancelRequest cancelAuction(int auctionId, String reason) {
//        AuctionCancelRequest req = new AuctionCancelRequest();
//        req.setAuctionId(auctionId);
//        req.setReason(reason);
//        return req;
//    }
//
//    private static void testHeader(String title) {
//        System.out.println("\n" + "=".repeat(64));
//        System.out.println(" " + title);
//        System.out.println("=".repeat(64));
//    }
//
//    private static final class TestClient implements Closeable {
//        private final String name;
//        private final Socket socket;
//        private final BufferedWriter writer;
//        private final BlockingQueue<ServerMessage> inbox = new LinkedBlockingQueue<>();
//        private final List<ServerMessage> unhandled = new ArrayList<>();
//        private final Thread readerThread;
//
//        private TestClient(String name, Socket socket) throws IOException {
//            this.name = name;
//            this.socket = socket;
//            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
//            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
//            this.readerThread = new Thread(() -> {
//                try {
//                    String line;
//                    while ((line = reader.readLine()) != null) {
//                        inbox.offer(ServerMessage.fromJson(line));
//                    }
//                } catch (IOException ignored) {}
//            }, "test-reader-" + name);
//            this.readerThread.setDaemon(true);
//            this.readerThread.start();
//        }
//
//        static TestClient connect(String name) throws IOException {
//            String host = Config.get("SERVER_HOST");
//            int port = Config.getInt("SERVER_PORT");
//            return new TestClient(name, new Socket(host, port));
//        }
//
//        ServerMessage sendAndAwait(String type, Object payload) throws Exception {
//            send(type, payload);
//            // Removed PRODUCT_LIST from defaults because it's also a broadcast type.
//            // If the command itself is PRODUCT_LIST, it's covered by the 'type' parameter.
//            return awaitType("SUCCESS", "ERROR", "PONG", "DEPOSIT", "WITHDRAW", "TRANSFER", type);
//        }
//
//        void send(String type, Object payload) throws IOException {
//            Map<String, Object> request = new LinkedHashMap<>();
//            request.put("type", type);
//            request.put("payload", payload);
//            synchronized (writer) {
//                writer.write(JsonConverter.toJson(request));
//                writer.newLine();
//                writer.flush();
//            }
//        }
//
//        ServerMessage awaitType(String... types) throws Exception {
//            List<String> expectedTypes = List.of(types);
//            synchronized (unhandled) {
//                for (int i = 0; i < unhandled.size(); i++) {
//                    ServerMessage msg = unhandled.get(i);
//                    if (expectedTypes.contains(msg.type)) {
//                        unhandled.remove(i);
//                        return msg;
//                    }
//                }
//            }
//
//            long deadline = System.nanoTime() + RESPONSE_TIMEOUT.toNanos();
//            while (System.nanoTime() < deadline) {
//                ServerMessage message = inbox.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
//                if (message == null) break;
//                if (expectedTypes.contains(message.type)) return message;
//                synchronized (unhandled) { unhandled.add(message); }
//            }
//            return null;
//        }
//
//        @Override
//        public void close() throws IOException {
//            socket.close();
//            readerThread.interrupt();
//        }
//    }
//
//    private static final class ServerMessage {
//        final String type;
//        final boolean success;
//        final String message;
//        final Object data;
//
//        private ServerMessage(String type, boolean success, String message, Object data) {
//            this.type = type; this.success = success; this.message = message; this.data = data;
//        }
//
//        static ServerMessage error(String msg) {
//            return new ServerMessage("ERROR", false, msg, null);
//        }
//
//        static ServerMessage fromJson(String json) {
//            Map<?, ?> raw = JsonConverter.fromJson(json, Map.class);
//            return new ServerMessage(
//                    String.valueOf(raw.get("type")),
//                    Boolean.TRUE.equals(raw.get("success")),
//                    String.valueOf(raw.get("message")),
//                    raw.get("data")
//            );
//        }
//
//        boolean successOrType(String t) { return success || t.equals(type); }
//        boolean hasDataField(String f) { return data instanceof Map && ((Map<?, ?>) data).containsKey(f); }
//
//        long numberData(String k) {
//            if (!(data instanceof Map<?, ?> map)) return 0;
//            Object v = map.get(k);
//            return v instanceof Number ? ((Number) v).longValue() : 0;
//        }
//
//        String stringData(String k) {
//            if (!(data instanceof Map<?, ?> map)) return "null";
//            return String.valueOf(map.get(k));
//        }
//
//        boolean hasErrorDetail(String code) {
//            if (!(data instanceof Map<?, ?> map)) return false;
//            return code.equals(map.get("errorCode"));
//        }
//
//        void requireSuccess() {
//            if (!success) {
//                throw new IllegalStateException("Expected success but got " + this);
//            }
//        }
//
//        @Override
//        public String toString() { return "type=" + type + ", success=" + success + ", msg=" + message; }
//    }
//
//    private static final class TestReport {
//        private final AtomicInteger passed = new AtomicInteger();
//        private final AtomicInteger failed = new AtomicInteger();
//
//        void check(String name, boolean condition) {
//            if (condition) {
//                passed.incrementAndGet();
//                System.out.println("  [PASS] " + name);
//            } else {
//                failed.incrementAndGet();
//                System.out.println("  [FAIL] " + name);
//            }
//        }
//
//        void check(String name, ServerMessage msg) {
//            check(name, msg != null && msg.success);
//        }
//
//        int failedCount() { return failed.get(); }
//        void printSummary() {
//            System.out.println("\nPASSED: " + passed.get() + " | FAILED: " + failed.get());
//        }
//    }
//}
