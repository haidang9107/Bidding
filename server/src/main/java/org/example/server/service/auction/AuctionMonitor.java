package org.example.server.service.auction;

import org.example.model.Auction;
import org.example.util.FileLogger;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Background worker that schedules precise auction-start and auction-end events.
 *
 * <p>Instead of polling the database, this monitor uses a
 * {@link ScheduledExecutorService} to fire the start/end logic exactly at the
 * scheduled times. A low-frequency safety net (every minute) still runs in case
 * the server was restarted and missed a deadline.
 */
public class AuctionMonitor {
    private final AuctionService auctionService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * Constructs an AuctionMonitor.
     * @param auctionService The auction service used to start / finish auctions.
     */
    public AuctionMonitor(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Schedules end-events for every auction currently in RUNNING state and starts the
     * recurring safety-net sweep.
     */
    public void start() {
        FileLogger.info("AuctionMonitor started: Monitoring precise auction ends.");

        try {
            List<Auction> runningAuctions = auctionService.getAllRunningAuctions();
            for (Auction a : runningAuctions) {
                scheduleAuctionEnd(a.getAuctionId(), a.getEndTime());
            }
            FileLogger.info("Scheduled " + runningAuctions.size() + " existing running auctions.");
        } catch (Exception e) {
            FileLogger.error("Failed to schedule existing running auctions on startup", e);
        }

        // Safety net: 1-minute sweep in case the precise scheduling missed something
        scheduler.scheduleAtFixedRate(
                () -> {
                    auctionService.processUpcomingAuctions();
                    auctionService.processExpiredAuctions();
                },
                1, 1, TimeUnit.MINUTES
        );
    }

    /**
     * Schedules a precise end-task for an auction. If a previous task exists (e.g. because
     * anti-snipping extended the end time), it is cancelled and replaced.
     *
     * @param auctionId The auction ID.
     * @param endTime   The auction's end time.
     */
    public void scheduleAuctionEnd(int auctionId, Timestamp endTime) {
        cancelTask(auctionId);

        long delay = endTime.getTime() - System.currentTimeMillis();
        if (delay < 0) delay = 0;

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                auctionService.processAuctionEnd(auctionId);
                scheduledTasks.remove(auctionId);
            } catch (Exception e) {
                FileLogger.error("Failed to process scheduled end for Auction " + auctionId, e);
            }
        }, delay, TimeUnit.MILLISECONDS);

        scheduledTasks.put(auctionId, future);
    }

    /**
     * Schedules a precise start-task for an auction.
     * @param auctionId The auction ID.
     * @param startTime The auction's start time.
     */
    public void scheduleAuctionStart(int auctionId, Timestamp startTime) {
        long delay = startTime.getTime() - System.currentTimeMillis();
        if (delay < 0) delay = 0;

        scheduler.schedule(() -> {
            try {
                auctionService.startAuction(auctionId);
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
     * Stops the background monitor gracefully.
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
