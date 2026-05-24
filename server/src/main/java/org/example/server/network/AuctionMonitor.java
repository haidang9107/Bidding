package org.example.server.network;

import org.example.model.product.Item;
import org.example.server.service.product.ProductService;
import org.example.util.FileLogger;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Giai đoạn 2: Giám sát đấu giá với cơ chế Schedule chính xác từng phiên.
 * 
 * Lớp này chịu trách nhiệm tự động kết thúc các phiên đấu giá khi hết thời gian.
 * Thay vì quét database liên tục, nó sử dụng ScheduledExecutorService để 
 * thực thi logic kết thúc chính xác tại thời điểm endTime.
 */
public class AuctionMonitor {
    private final ProductService productService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * Constructs an AuctionMonitor with the specified ProductService.
     * @param productService the product service to use for processing auctions
     */
    public AuctionMonitor(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Starts the background monitoring task for recurring maintenance.
     * Mặc dù sử dụng Schedule chính xác, hệ thống vẫn duy trì một task quét 
     * định kỳ (1 phút/lần) như một lớp "phòng thủ" để xử lý các phiên đấu giá
     * bị sót (ví dụ: do Server bị restart).
     */
    public void start() {
        FileLogger.info("AuctionMonitor started: Monitoring precise auction ends.");
        
        // 1. Lấy tất cả các phiên đang chạy để lập lịch chính xác ngay khi khởi động
        try {
            List<Item> runningAuctions = productService.getAllRunningAuctions();
            for (org.example.model.product.Item item : runningAuctions) {
                scheduleAuctionEnd(item.getAuctionId(), item.getEndTime());
            }
            FileLogger.info("Scheduled " + runningAuctions.size() + " existing running auctions.");
        } catch (Exception e) {
            FileLogger.error("Failed to schedule existing running auctions on startup", e);
        }

        // 2. Vẫn giữ task quét định kỳ 1 phút để "phòng thủ"
        scheduler.scheduleAtFixedRate(
            () -> {
                productService.processUpcomingAuctions();
                productService.processExpiredAuctions();
            },
            1, 1, TimeUnit.MINUTES
        );
    }

    /**
     * Lập lịch kết thúc chính xác cho một phiên đấu giá.
     * Nếu phiên này đã có lịch cũ, lịch cũ sẽ bị hủy và thay thế bằng lịch mới 
     * (thường xảy ra khi thời gian kết thúc bị kéo dài do Anti-snipping).
     * 
     * @param auctionId ID phiên đấu giá.
     * @param endTime Thời điểm kết thúc chính xác của phiên.
     */
    public void scheduleAuctionEnd(int auctionId, Timestamp endTime) {
        // Hủy task cũ nếu có (trường hợp bị anti-snipping kéo dài thời gian)
        cancelTask(auctionId);

        long delay = endTime.getTime() - System.currentTimeMillis();
        if (delay < 0) delay = 0;

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                productService.processAuctionEnd(auctionId);
                scheduledTasks.remove(auctionId);
            } catch (Exception e) {
                FileLogger.error("Failed to process scheduled end for Auction " + auctionId, e);
            }
        }, delay, TimeUnit.MILLISECONDS);

        scheduledTasks.put(auctionId, future);
    }

    /**
     * Lập lịch bắt đầu chính xác cho một phiên đấu giá.
     * @param auctionId ID phiên đấu giá.
     * @param startTime Thời điểm bắt đầu chính xác của phiên.
     */
    public void scheduleAuctionStart(int auctionId, Timestamp startTime) {
        long delay = startTime.getTime() - System.currentTimeMillis();
        if (delay < 0) delay = 0;

        scheduler.schedule(() -> {
            try {
                productService.startAuction(auctionId);
            } catch (Exception e) {
                FileLogger.error("Failed to process scheduled start for Auction " + auctionId, e);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void cancelTask(int auctionId) {
        ScheduledFuture<?> future = scheduledTasks.remove(auctionId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    /**
     * Stops the background monitoring task gracefully.
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
