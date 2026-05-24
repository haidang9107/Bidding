package org.example.server.network;

import org.example.server.service.product.ProductService;
import org.example.util.FileLogger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background task to monitor auction end times and handle automatic closures.
 */
public class AuctionMonitor {
    private final ProductService productService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Constructs an AuctionMonitor with the specified ProductService.
     *
     * @param productService the product service to use for processing auctions
     */
    public AuctionMonitor(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Starts the background monitoring task.
     * It periodically checks for expired and upcoming auctions.
     */
    public void start() {
        FileLogger.info("AuctionMonitor started: Checking for expired auctions every 5 seconds.");
        
        // Check every 5 seconds for expired or starting auctions
        scheduler.scheduleAtFixedRate(
            () -> {
                productService.processUpcomingAuctions();
                productService.processExpiredAuctions();
            },
            5, 5, TimeUnit.SECONDS
        );
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
