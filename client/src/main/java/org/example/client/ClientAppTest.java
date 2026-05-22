package org.example.client;

import org.example.util.Config;
import org.example.util.JsonConverter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Socket-level integration test runner for the bidding server.
 * It uses queues, latches, futures, and timeouts for deterministic coordination.
 */
public class ClientAppTest {
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(5);

    public static void main(String[] args) {
        TestReport report = new TestReport();

        try {
            testHeader("SCENARIO 1: Basic protocol and auth");
            try (TestClient admin = TestClient.connect("admin")) {
                report.check("PING returns PONG/SUCCESS",
                        admin.sendAndAwait("PING", null).successOrType("PONG"));
                report.check("Admin login succeeds",
                        admin.sendAndAwait("LOGIN", login("admin_minh", "password123")).success);
                report.check("Admin can activate huong_member",
                        admin.sendAndAwait("ADMIN_BAN_USER", adminUser("huong_member", 0)).success);
                report.check("Admin can activate nam_member",
                        admin.sendAndAwait("ADMIN_BAN_USER", adminUser("nam_member", 0)).success);
                report.check("Admin can activate apple_store",
                        admin.sendAndAwait("ADMIN_BAN_USER", adminUser("apple_store", 0)).success);
            }

            int auctionId = createFreshAuction(report);

            testHeader("SCENARIO 2: Member setup, rooms, finance");
            try (TestClient huong = TestClient.connect("huong")) {
                report.check("Huong login succeeds",
                        huong.sendAndAwait("LOGIN", login("huong_member", "password123")).success);
                report.check("Huong joins auction room",
                        huong.sendAndAwait("JOIN_AUCTION_ROOM", room(auctionId)).success);
                report.check("Deposit for bidding succeeds",
                        huong.sendAndAwait("DEPOSIT", 50_000_000L).success);
                report.check("Product list is protected and works after login",
                        huong.sendAndAwait("PRODUCT_LIST", pagination(1, 5)).success);
            }

            testHeader("SCENARIO 3: Auto-bid");
            try (TestClient huong = TestClient.connect("auto-huong");
                 TestClient nam = TestClient.connect("auto-nam")) {
                report.check("Huong login for auto-bid",
                        huong.sendAndAwait("LOGIN", login("huong_member", "password123")).success);
                report.check("Nam login for manual competing bid",
                        nam.sendAndAwait("LOGIN", login("nam_member", "password123")).success);
                report.check("Huong joins auction room for auto-bid updates",
                        huong.sendAndAwait("JOIN_AUCTION_ROOM", room(auctionId)).success);
                report.check("Nam joins auction room for auto-bid updates",
                        nam.sendAndAwait("JOIN_AUCTION_ROOM", room(auctionId)).success);
                report.check("Nam deposit succeeds",
                        nam.sendAndAwait("DEPOSIT", 50_000_000L).success);
                huong.send("AUTO_BID_SET", autoBid(auctionId, 35_000_000L, 500_000L));
                report.check("Huong configures auto-bid",
                        huong.awaitType("SUCCESS").success);

                nam.send("BID_PLACE", bid(auctionId, 30_000_000L));
                ServerMessage manualBidResponse = nam.awaitType("SUCCESS");
                report.check("Manual bid against auto-bid endpoint succeeds", manualBidResponse.success);

                ServerMessage update = huong.awaitType("BID_UPDATE");
                report.check("Room receives BID_UPDATE after auto-bid/manual bid", update.success);
                report.check("BID_UPDATE stays scoped to auction room",
                        Objects.equals(update.numberData("auctionId"), Long.valueOf(auctionId)));
            }

            testHeader("SCENARIO 4: Concurrent bidding with multiple client connections");
            runConcurrentBidTest(report, auctionId);

            testHeader("SUMMARY");
            report.printSummary();
            if (report.failedCount() > 0) {
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static int createFreshAuction(TestReport report) throws Exception {
        testHeader("SCENARIO 1B: Create fresh auction for deterministic tests");
        try (TestClient seller = TestClient.connect("seller")) {
            report.check("Seller login succeeds",
                    seller.sendAndAwait("LOGIN", login("apple_store", "password123")).success);
            seller.send("PRODUCT_ADD", product(
                    "Integration Test Laptop " + System.currentTimeMillis(),
                    "Created by ClientApp integration test",
                    25_000_000L,
                    "ELECTRONICS"
            ));
            ServerMessage addResponse = seller.awaitType("SUCCESS");
            report.check("Seller creates test auction", addResponse.success);
            ServerMessage broadcast = seller.awaitType("PRODUCT_LIST");
            Long auctionId = broadcast.numberData("auctionId");
            if (auctionId == null || auctionId <= 0) {
                throw new IllegalStateException("Could not read auctionId from product broadcast: " + broadcast);
            }
            System.out.println("Using fresh auctionId=" + auctionId);
            return auctionId.intValue();
        }
    }

    private static void runConcurrentBidTest(TestReport report, int auctionId) throws Exception {
        int clientCount = 6;
        long baseAmount = 36_000_000L;
        ExecutorService executor = Executors.newFixedThreadPool(clientCount);
        CyclicBarrier startTogether = new CyclicBarrier(clientCount);
        CountDownLatch done = new CountDownLatch(clientCount);
        List<Callable<ServerMessage>> tasks = new ArrayList<>();

        for (int i = 0; i < clientCount; i++) {
            final int index = i;
            tasks.add(() -> {
                try (TestClient client = TestClient.connect("bidder-" + index)) {
                    String account = index % 2 == 0 ? "nam_member" : "huong_member";
                    client.sendAndAwait("LOGIN", login(account, "password123")).requireSuccess();
                    client.sendAndAwait("JOIN_AUCTION_ROOM", room(auctionId)).requireSuccess();
                    client.sendAndAwait("DEPOSIT", 20_000_000L).requireSuccess();
                    startTogether.await(5, TimeUnit.SECONDS);
                    return client.sendAndAwait("BID_PLACE", bid(auctionId, baseAmount + index * 500_000L));
                } finally {
                    done.countDown();
                }
            });
        }

        List<java.util.concurrent.Future<ServerMessage>> futures = executor.invokeAll(tasks, 10, TimeUnit.SECONDS);
        boolean completed = done.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        int successCount = 0;
        int failureCount = 0;
        for (java.util.concurrent.Future<ServerMessage> future : futures) {
            if (!future.isDone() || future.isCancelled()) {
                failureCount++;
                continue;
            }
            ServerMessage message = future.get();
            if (message.success) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        report.check("All concurrent bid tasks completed", completed);
        report.check("At least one concurrent bid succeeds", successCount > 0);
        report.check("At least one concurrent bid is rejected or loses race", failureCount > 0);
        System.out.println("Concurrent bid results: success=" + successCount + ", rejected=" + failureCount);
    }

    private static Map<String, Object> login(String username, String password) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        return payload;
    }

    private static Map<String, Object> adminUser(String accountname, int status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("targetAccountname", accountname);
        payload.put("status", status);
        return payload;
    }

    private static Map<String, Object> pagination(int page, int pageSize) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("page", page);
        payload.put("pageSize", pageSize);
        return payload;
    }

    private static Map<String, Object> product(String name, String description,
                                               long startingPrice, String category) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("description", description);
        payload.put("startingPrice", startingPrice);
        payload.put("category", category);
        return payload;
    }

    private static Map<String, Object> room(int auctionId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("auctionId", auctionId);
        return payload;
    }

    private static Map<String, Object> bid(int auctionId, long amount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("auctionId", auctionId);
        payload.put("amount", amount);
        return payload;
    }

    private static Map<String, Object> autoBid(int auctionId, long maxBid, long incrementAmount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("auctionId", auctionId);
        payload.put("maxBid", maxBid);
        payload.put("incrementAmount", incrementAmount);
        return payload;
    }

    private static void testHeader(String title) {
        System.out.println("\n" + "=".repeat(72));
        System.out.println(" " + title);
        System.out.println("=".repeat(72));
    }

    private static final class TestClient implements Closeable {
        private final String name;
        private final Socket socket;
        private final BufferedWriter writer;
        private final BlockingQueue<ServerMessage> inbox = new LinkedBlockingQueue<>();
        private final List<ServerMessage> unhandled = new ArrayList<>();
        private final Thread readerThread;

        private TestClient(String name, Socket socket) throws IOException {
            this.name = name;
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            this.readerThread = new Thread(() -> readLoop(reader), "test-client-reader-" + name);
            this.readerThread.setDaemon(true);
            this.readerThread.start();
        }

        static TestClient connect(String name) throws IOException {
            String host = Config.get("SERVER_HOST");
            int port = Config.getInt("SERVER_PORT");
            Socket socket = new Socket(host, port);
            socket.setTcpNoDelay(true);
            return new TestClient(name, socket);
        }

        ServerMessage sendAndAwait(String type, Object payload) throws Exception {
            send(type, payload);
            return awaitType("SUCCESS", "ERROR", "PONG", type);
        }

        void send(String type, Object payload) throws IOException {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("type", type);
            request.put("payload", payload);
            synchronized (writer) {
                writer.write(JsonConverter.toJson(request));
                writer.newLine();
                writer.flush();
            }
            System.out.println("[" + name + " ->] " + type + " " + payload);
        }

        ServerMessage awaitNext() throws Exception {
            ServerMessage message = inbox.poll(RESPONSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (message == null) {
                throw new IllegalStateException("Timed out waiting for response for client " + name);
            }
            System.out.println("[" + name + " <-] " + message);
            return message;
        }

        ServerMessage awaitType(String... types) throws Exception {
            List<String> expectedTypes = List.of(types);
            synchronized (unhandled) {
                for (int i = 0; i < unhandled.size(); i++) {
                    ServerMessage msg = unhandled.get(i);
                    if (expectedTypes.contains(msg.type)) {
                        unhandled.remove(i);
                        System.out.println("[" + name + " <- cached] " + msg);
                        return msg;
                    }
                }
            }

            long deadline = System.nanoTime() + RESPONSE_TIMEOUT.toNanos();
            while (System.nanoTime() < deadline) {
                long remaining = deadline - System.nanoTime();
                ServerMessage message = inbox.poll(remaining, TimeUnit.NANOSECONDS);
                if (message == null) break;
                System.out.println("[" + name + " <-] " + message);
                if (expectedTypes.contains(message.type)) {
                    return message;
                } else {
                    synchronized (unhandled) {
                        unhandled.add(message);
                    }
                }
            }
            throw new IllegalStateException("Timed out waiting for " + expectedTypes + " for client " + name);
        }

        private void readLoop(BufferedReader reader) {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    inbox.offer(ServerMessage.fromJson(line));
                }
            } catch (IOException ignored) {
                // Normal during close.
            }
        }

        @Override
        public void close() throws IOException {
            socket.close();
            readerThread.interrupt();
        }
    }

    private static final class ServerMessage {
        private final String type;
        private final boolean success;
        private final String message;
        private final Object data;

        private ServerMessage(String type, boolean success, String message, Object data) {
            this.type = type;
            this.success = success;
            this.message = message;
            this.data = data;
        }

        static ServerMessage fromJson(String json) {
            Map<?, ?> raw = JsonConverter.fromJson(json, Map.class);
            return new ServerMessage(
                    String.valueOf(raw.get("type")),
                    Boolean.TRUE.equals(raw.get("success")),
                    String.valueOf(raw.get("message")),
                    raw.get("data")
            );
        }

        boolean successOrType(String expectedType) {
            return success || expectedType.equals(type);
        }

        void requireSuccess() {
            if (!success) {
                throw new IllegalStateException("Expected success but got " + this);
            }
        }

        Long numberData(String key) {
            if (!(data instanceof Map<?, ?> map)) {
                return null;
            }
            Object value = map.get(key);
            if (value instanceof Number number) {
                return number.longValue();
            }
            return null;
        }

        @Override
        public String toString() {
            return "type=" + type + ", success=" + success + ", message=" + message + ", data=" + data;
        }
    }

    private static final class TestReport {
        private final AtomicInteger passed = new AtomicInteger();
        private final AtomicInteger failed = new AtomicInteger();

        void check(String name, boolean condition) {
            if (condition) {
                passed.incrementAndGet();
                System.out.println("[PASS] " + name);
            } else {
                failed.incrementAndGet();
                System.out.println("[FAIL] " + name);
            }
        }

        int failedCount() {
            return failed.get();
        }

        void printSummary() {
            System.out.println("Passed: " + passed.get());
            System.out.println("Failed: " + failed.get());
        }
    }
}
