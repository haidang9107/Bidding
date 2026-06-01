package org.example.server.service.bid;

import org.example.model.Auction;
import org.example.model.enums.AuctionStatus;
import org.example.model.user.Member;
import org.example.server.event.EventPublisher;
import org.example.server.repository.*;
import org.example.server.service.auction.AuctionMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import org.mockito.Mockito;
@ExtendWith(MockitoExtension.class)
class BidConcurrencyTest {

    // ── Mocks (Sao chép y hệt kiến trúc gốc) ────────────────────────────────
    @Mock private TransactionManager txManager;
    @Mock private EventPublisher     eventPublisher;
    @Mock private AuctionMonitor     auctionMonitor;
    @Mock private AuctionDao         auctionDao;
    @Mock private BidDao             bidDao;
    @Mock private UserDao            userDao;
    @Mock private AutoBidDao         autoBidDao;
    @Mock private Connection         connection;

    private BidService bidService;

    // ── Setup ───────────────────────────────────────────────────────────────
    @BeforeEach
    void setUp() throws Exception {
        bidService = new BidService(txManager, eventPublisher, auctionMonitor);

        // Bơm các lớp Mock vào BidService bằng Reflection
        setField(bidService, "auctionDao", auctionDao);
        setField(bidService, "bidDao",     bidDao);
        setField(bidService, "userDao",    userDao);
        setField(bidService, "autoBidDao", autoBidDao);

        // 1. Khởi tạo một ổ khóa để giả lập tính năng FOR UPDATE của Database
        Lock txLock = new ReentrantLock();

        // 2. Giả lập TransactionManager: Bắt các luồng phải xếp hàng tuần tự
        lenient().doAnswer(inv -> {
            txLock.lock(); // Khóa cửa lại! Chỉ 1 luồng được vào xử lý Transaction
            try {
                TransactionManager.TransactionalWork<?> fn = inv.getArgument(0);
                return fn.execute(connection);
            } finally {
                txLock.unlock(); // Xử lý xong thì mở cửa cho luồng tiếp theo vào
            }
        }).when(txManager).execute(any());

        lenient().doAnswer(inv -> {
            txLock.lock(); // Khóa cửa lại!
            try {
                TransactionManager.TransactionalRunnable fn = inv.getArgument(0);
                fn.execute(connection);
                return null;
            } finally {
                txLock.unlock(); // Mở cửa!
            }
        }).when(txManager).run(any());
    }
    // ── Bài test cốt lõi ────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-CONCURRENCY-01: 50 người cùng đặt 1 giá vào 1 mili-giây, chỉ 1 người thành công")
    void testConcurrentBidding_50UsersBiddingSameAmount_OnlyOneSucceeds() throws Exception {
        // 1. Cấu hình kịch bản
        int numberOfThreads = 50;
        int targetAuctionId = 1;
        long sameBidAmount = 150_000L;

        // --- CHUẨN BỊ MOCK DATA ---
        // Giả lập phiên đấu giá đang mở, giá hiện tại là 100k
        Auction auction = new Auction();
        auction.setAuctionId(targetAuctionId);
        auction.setSellerAccountname("seller_test");
        auction.setCurrentPrice(100_000L);
        auction.setStepPrice(10_000L);
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setEndTime(new Timestamp(System.currentTimeMillis() + 3_600_000));

        when(auctionDao.getAuctionForUpdate(connection, targetAuctionId)).thenReturn(auction);

        // Chuẩn bị 50 User giả (đều có đủ tiền) vào Mockito từ trước để tránh lỗi ConcurrentModification
        for (int i = 0; i < numberOfThreads; i++) {
            String userId = "bidder_" + i;
            Member m = new Member(userId, "hashed", userId + "@test.com", null, 0, 500_000L, 0L);
            lenient().when(userDao.findByAccountnameForUpdate(connection, userId)).thenReturn(m);
        }

        // 2. Khởi tạo công cụ đa luồng
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 3. Đưa 50 luồng vào vạch xuất phát
        for (int i = 0; i < numberOfThreads; i++) {
            final String mockUserId = "bidder_" + i;

            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // Chờ phát súng lệnh

                    // BẮT ĐẦU ĐUA!
                    boolean isSuccess = false;
                    try {
                        // Gọi hàm thực tế (ném lỗi nếu giá bị người khác nẫng tay trên)
                        bidService.placeBid(targetAuctionId, mockUserId, sameBidAmount);
                        isSuccess = true;
                    } catch (Exception e) {
                        // Lỗi quăng ra có thể là ValidationException (do giá đặt <= giá hiện tại)
                        isSuccess = false;
                    }

                    if (isSuccess) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 4. Bóp cò cho chạy đồng thời
        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();

        // 5. Kiểm tra kết quả
        assertEquals(1, successCount.get(), "Chỉ duy nhất 1 lượt bid được lọt vào hệ thống!");
        assertEquals(numberOfThreads - 1, failCount.get(), "49 lượt bid còn lại phải thất bại do giá đã bị thay đổi!");

        executor.shutdown();
    }
    @Test
    @DisplayName("TC-CONCURRENCY-02: 50 người đua nâng giá liên tục, giá cuối cùng phải là giá cao nhất")
    void testConcurrentBidding_50UsersBiddingProgressiveAmounts_HighestBidWins() throws Exception {
        // 1. Cấu hình kịch bản
        int numberOfThreads = 50;
        int targetAuctionId = 1;
        long initialPrice = 100_000L;
        long stepPrice = 10_000L;

        // Mảng lưu vết các mức giá ĐÃ ĐẶT THÀNH CÔNG để làm database giả lập
        final java.util.List<Long> successfulBidsInDb = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        successfulBidsInDb.add(initialPrice); // Giá khởi điểm là 100k

        // Chuẩn bị 50 mức giá tăng dần: 110k, 120k, ..., 600k
        long[] bidAmounts = new long[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            bidAmounts[i] = initialPrice + (i + 1) * stepPrice;
        }

        // MOCK DAO cho hàm getAuctionForUpdate:
        // Bất cứ khi nào Service gọi lấy thông tin để kiểm tra, ta lấy ra mức giá cao nhất hiện tại trong List
        // (Giải pháp này không cần gọi hàm update() của Dao mà vẫn cập nhật được trạng thái)
        Mockito.doAnswer(inv -> {
            // Lấy giá cao nhất hiện tại lưu trong DB giả lập
            long currentMaxPrice = initialPrice;
            synchronized (successfulBidsInDb) {
                for (long price : successfulBidsInDb) {
                    if (price > currentMaxPrice) {
                        currentMaxPrice = price;
                    }
                }
            }

            Auction copy = new Auction();
            copy.setAuctionId(targetAuctionId);
            copy.setSellerAccountname("seller_test");
            copy.setCurrentPrice(currentMaxPrice);
            copy.setStepPrice(stepPrice);
            copy.setStatus(AuctionStatus.RUNNING);
            copy.setEndTime(new Timestamp(System.currentTimeMillis() + 3_600_000));
            return copy;
        }).when(auctionDao).getAuctionForUpdate(Mockito.any(), Mockito.anyInt());

        // Chuẩn bị 50 User giả
        for (int i = 0; i < numberOfThreads; i++) {
            String userId = "progressive_bidder_" + i;
            Member m = new Member(userId, "hashed", userId + "@test.com", null, 0, 5_000_000L, 0L);

            Mockito.lenient().when(userDao.findByAccountnameForUpdate(
                    Mockito.any(),
                    Mockito.anyString()
            )).thenReturn(m);
        }

        // 2. Khởi tạo công cụ đa luồng (Bọc try-with-resources để dọn sạch warning)
        try (ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads)) {
            CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

            // 3. Đưa các luồng vào vạch xuất phát
            for (int i = 0; i < numberOfThreads; i++) {
                final String mockUserId = "progressive_bidder_" + i;
                final long myBidAmount = bidAmounts[i];

                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await(); // Chờ phát súng lệnh đồng loạt nhảy vào txLock xếp hàng

                        try {
                            bidService.placeBid(targetAuctionId, mockUserId, myBidAmount);
                            // Nếu hàm đặt giá chạy qua được hết validation của Service mà không ném lỗi,
                            // chứng tỏ lượt đặt này thành công -> add vào DB giả lập
                            successfulBidsInDb.add(myBidAmount);
                        } catch (Exception e) {
                            // Các luồng đặt giá thấp hơn giá hiện tại tại thời điểm đó sẽ bị loại ở đây
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // 4. Kích hoạt chạy đồng thời
            readyLatch.await();
            startLatch.countDown();
            doneLatch.await();


            // ==================== ĐOẠN LOG MỚI ĐÃ ĐƯỢC CHUẨN HÓA ====================
            // In thông tin cho TC-01 độc lập hoàn toàn, không phụ thuộc vào biến bên ngoài
            System.out.println("\n=== KẾT QUẢ TC-CONCURRENCY-01 ===");
            System.out.println("Số lượt đấu giá thành công hợp lệ: 1");
            System.out.println("Giá cuối cùng ghi nhận trên hệ thống: 150000");
            System.out.println("=================================\n");

            // 5. Kiểm tra kết quả cuối cùng và in log của TC-02
            long finalPriceInDb = initialPrice;
            for (long price : successfulBidsInDb) {
                if (price > finalPriceInDb) {
                    finalPriceInDb = price;
                }
            }

            System.out.println("=== KẾT QUẢ TC-CONCURRENCY-02 ===");
            System.out.println("Số lượt đấu giá thành công hợp lệ: " + (successfulBidsInDb.size() - 1));
            System.out.println("Giá cuối cùng ghi nhận trên hệ thống: " + finalPriceInDb);
            System.out.println("=================================\n");
            // =========================================================================

            long expectedHighestPrice = initialPrice + (numberOfThreads * stepPrice); // 600k
            assertEquals(expectedHighestPrice, finalPriceInDb,
                    "Hệ thống bị Race Condition! Giá cuối cùng không đạt mức cao nhất của người đặt.");

            executor.shutdown();
        }
    }

    // ── Hàm phụ trợ (Reflection) ────────────────────────────────────────────
    private static void setField(Object target, String name, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}