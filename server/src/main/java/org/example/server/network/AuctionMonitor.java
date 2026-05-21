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

    public AuctionMonitor(ProductService productService) {
        this.productService = productService;
    }

    public void start() {
        FileLogger.info("AuctionMonitor started: Checking for expired auctions every 5 seconds.");
        
        // Check every 5 seconds
        scheduler.scheduleAtFixedRate(
            productService::processExpiredAuctions,
            5, 5, TimeUnit.SECONDS
        );
    }

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
