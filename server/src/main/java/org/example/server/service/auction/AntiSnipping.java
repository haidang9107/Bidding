package org.example.server.service.auction;

import org.example.model.Auction;
import org.example.server.repository.AuctionDao;
import org.example.util.Config;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Anti-snipping rule: when a bid lands close to the end of an auction,
 * extend the auction's end time by a configured amount so other bidders
 * have a chance to react.
 */
public class AntiSnipping {
    private static final long SNIP_WINDOW_MS = Config.getInt("ANTI_SNIP_WINDOW_MS");
    private static final long EXTENSION_MS = Config.getInt("ANTI_SNIP_EXTENSION_MS");

    /**
     * Checks if the most recent bid is inside the snipping window and, if so,
     * extends the auction's end time both in memory and in the database.
     *
     * @param connection  The database connection.
     * @param auction     The auction being bid on (must already be row-locked).
     * @param auctionDao  The DAO used to update the end time.
     * @throws SQLException If a database error occurs.
     */
    public static void process(Connection connection, Auction auction, AuctionDao auctionDao) throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp endTime = auction.getEndTime();
        if (endTime == null) return;

        long diff = endTime.getTime() - now.getTime();
        if (diff > 0 && diff <= SNIP_WINDOW_MS) {
            Timestamp newEndTime = new Timestamp(endTime.getTime() + EXTENSION_MS);
            auction.setEndTime(newEndTime);

            boolean updated = auctionDao.updateAuctionEndTime(connection, auction.getAuctionId(), newEndTime);
            if (updated) {
                FileLogger.info("Anti-snipping triggered for Auction " + auction.getAuctionId()
                        + ". New end time: " + newEndTime);
            }
        }
    }
}
