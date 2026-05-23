package org.example.server.service.bid;

import org.example.model.product.Item;
import org.example.server.repository.ProductDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Implementation of anti-snipping logic to extend auction end time
 * when a bid is placed near the deadline.
 */
public class AntiSnipping {
    private static final long SNIP_WINDOW_MS = 60 * 1000; // 1 minute
    private static final long EXTENSION_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Checks if the bid was placed within the snipping window and extends the auction end time if so.
     *
     * @param connection the database connection
     * @param item       the item being auctioned
     * @param productDao the DAO to update the end time
     * @throws SQLException if a database error occurs
     */
    public static void process(Connection connection, Item item, ProductDao productDao) throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp endTime = item.getEndTime();

        if (endTime == null) return;

        long diff = endTime.getTime() - now.getTime();

        // If bid is within SNIP_WINDOW_MS of the end time
        if (diff > 0 && diff <= SNIP_WINDOW_MS) {
            Timestamp newEndTime = new Timestamp(endTime.getTime() + EXTENSION_MS);
            item.setEndTime(newEndTime);
            
            boolean updated = productDao.updateAuctionEndTime(connection, item.getAuctionId(), newEndTime);
            
            if (updated) {
                FileLogger.info("Anti-snipping triggered for Auction " + item.getAuctionId() 
                    + ". New end time: " + newEndTime);
            }
        }
    }
}
