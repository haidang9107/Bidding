package org.example.server.network;

import org.example.util.FileLogger;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SOLID: Single Responsibility - Periodically checks for inactive connections.
 */
public class InactivityMonitor {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final long timeoutMillis;

    /**
     * Constructs an InactivityMonitor with specified timeout.
     * @param timeoutSeconds the inactivity timeout in seconds
     */
    public InactivityMonitor(long timeoutSeconds) {
        this.timeoutMillis = timeoutSeconds * 1000;
    }

    /**
     * Starts the inactivity monitoring task.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(this::checkInactivity, 10, 10, TimeUnit.SECONDS);
        FileLogger.info("InactivityMonitor started with timeout: " + (timeoutMillis / 1000) + "s");
    }

    /**
     * Stops the inactivity monitoring task.
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    /**
     * Checks all heartbeat timestamps and disconnects inactive channels.
     */
    private void checkInactivity() {
        long now = System.currentTimeMillis();
        Map<SocketChannel, Long> lastActiveTimes = HeartbeatRegistry.getAll();

        for (Map.Entry<SocketChannel, Long> entry : lastActiveTimes.entrySet()) {
            SocketChannel channel = entry.getKey();
            long lastActive = entry.getValue();

            if (now - lastActive > timeoutMillis) {
                FileLogger.warn("Channel timed out due to inactivity: " + channel);
                DisconnectionHandler.handle(channel);
            }
        }
    }
}
