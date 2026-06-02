package org.example.server.service.auction;

import org.example.model.Auction;
import org.example.server.service.user.WatchlistService;
import org.example.util.FileLogger;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Background worker that schedules precise auction-start and auction-end events.
 */
public class AuctionMonitor {
    private static final long WARNING_BEFORE_END_MS = 5 * 60 * 1000; // 5 minutes

    private final AuctionService auctionService;
    private WatchlistService watchlistService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> warningTasks = new ConcurrentHashMap<>();

    public AuctionMonitor(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void setWatchlistService(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    public void start() {
        FileLogger.info("AuctionMonitor started.");
        try {
            List<Auction> runningAuctions = auctionService.getAllRunningAuctions();
            for (Auction a : runningAuctions) {
                scheduleAuctionEnd(a.getAuctionId(), a.getEndTime());
            }
        } catch (Exception e) {
            FileLogger.error("Failed to schedule existing running auctions", e);
        }

        scheduler.scheduleAtFixedRate(() -> {
            auctionService.processUpcomingAuctions();
            auctionService.processExpiredAuctions();
        }, 1, 1, TimeUnit.MINUTES);
    }

    public void scheduleAuctionEnd(int auctionId, Timestamp endTime) {
        cancelTask(auctionId);
        cancelWarningTask(auctionId);

        long now = System.currentTimeMillis();
        long delay = endTime.getTime() - now;
        if (delay < 0) delay = 0;

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                auctionService.processAuctionEnd(auctionId);
                scheduledTasks.remove(auctionId);
            } catch (Exception e) {
                FileLogger.error("Error processing end for Auction " + auctionId, e);
            }
        }, delay, TimeUnit.MILLISECONDS);
        scheduledTasks.put(auctionId, future);

        long warningDelay = delay - WARNING_BEFORE_END_MS;
        if (warningDelay > 0) {
            ScheduledFuture<?> warningFuture = scheduler.schedule(() -> {
                try {
                    sendNearingEndNotifications(auctionId);
                    warningTasks.remove(auctionId);
                } catch (Exception e) {
                    FileLogger.error("Error sending warning for Auction " + auctionId, e);
                }
            }, warningDelay, TimeUnit.MILLISECONDS);
            warningTasks.put(auctionId, warningFuture);
        }
    }

    private void sendNearingEndNotifications(int auctionId) {
        if (watchlistService == null) return;
        Auction auction = auctionService.getAuctionById(auctionId);
        if (auction == null || auction.getStatus() != org.example.model.enums.AuctionStatus.RUNNING) return;

        notifyWatchers(auction.getProductId(), 
            "Cuộc đấu giá cho '" + auction.getProduct().getName() + "' sắp kết thúc (còn 5 phút)!");
    }

    /**
     * Notifies all users watching a specific product with a given message.
     * Used for "Auction Started" and "Auction Ending Soon" alerts.
     * @param productId The ID of the product.
     * @param message   The notification message text.
     */
    public void notifyWatchers(int productId, String message) {
        if (watchlistService == null) return;
        List<String> watchers = watchlistService.getUsersWatchingProduct(productId);
        for (String accountname : watchers) {
            org.example.server.network.Broadcaster.sendToUser(accountname, new org.example.payload.Response<>(
                    org.example.model.enums.MessageType.NOTIFICATION, true, message, null));
        }
    }

    public void scheduleAuctionStart(int auctionId, Timestamp startTime) {
        long delay = startTime.getTime() - System.currentTimeMillis();
        if (delay < 0) delay = 0;
        scheduler.schedule(() -> {
            try {
                auctionService.startAuction(auctionId);
            } catch (Exception e) {
                FileLogger.error("Error starting Auction " + auctionId, e);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void cancelTask(int auctionId) {
        ScheduledFuture<?> future = scheduledTasks.remove(auctionId);
        if (future != null) future.cancel(false);
    }

    private void cancelWarningTask(int auctionId) {
        ScheduledFuture<?> future = warningTasks.remove(auctionId);
        if (future != null) future.cancel(false);
    }

    public void stop() {
        scheduler.shutdown();
    }
}
