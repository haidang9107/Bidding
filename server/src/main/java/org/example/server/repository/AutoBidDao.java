package org.example.server.repository;

import org.example.model.AutoBid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for automatic bid configurations.
 */
public class AutoBidDao {

    /**
     * Inserts or updates an automatic bid configuration.
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @param bidderAccountname The account name of the bidder.
     * @param maxBid The maximum amount for auto-bidding.
     * @param incrementAmount The increment amount.
     * @return True if successful.
     * @throws SQLException If a database error occurs.
     */
    public boolean upsertAutoBid(Connection connection, int auctionId, String bidderAccountname,
                                 long maxBid, long incrementAmount) throws SQLException {
        String updateSql = """
                UPDATE auto_bids
                SET max_bid = ?, increment_amount = ?, active = TRUE, updated_at = CURRENT_TIMESTAMP
                WHERE auction_id = ? AND bidder_accountname = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setLong(1, maxBid);
            ps.setLong(2, incrementAmount);
            ps.setInt(3, auctionId);
            ps.setString(4, bidderAccountname);
            if (ps.executeUpdate() > 0) {
                return true;
            }
        }

        String insertSql = """
                INSERT INTO auto_bids(auction_id, bidder_accountname, max_bid, increment_amount, active)
                VALUES (?, ?, ?, ?, TRUE)
                """;
        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            ps.setInt(1, auctionId);
            ps.setString(2, bidderAccountname);
            ps.setLong(3, maxBid);
            ps.setLong(4, incrementAmount);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deactivates an automatic bid for a specific bidder and auction.
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @param bidderAccountname The account name of the bidder.
     * @return True if successful.
     * @throws SQLException If a database error occurs.
     */
    public boolean deactivateAutoBid(Connection connection, int auctionId, String bidderAccountname)
            throws SQLException {
        String sql = """
                UPDATE auto_bids
                SET active = FALSE, updated_at = CURRENT_TIMESTAMP
                WHERE auction_id = ? AND bidder_accountname = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setString(2, bidderAccountname);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Finds all active automatic bids for a specific auction, ordered by priority.
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @return A list of active auto-bids.
     * @throws SQLException If a database error occurs.
     */
    public List<AutoBid> findAllActiveForAuction(Connection connection, int auctionId) throws SQLException {
        List<AutoBid> autoBids = new ArrayList<>();
        String sql = """
                SELECT *
                FROM auto_bids
                WHERE auction_id = ? AND active = TRUE
                ORDER BY max_bid DESC, created_at ASC
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    autoBids.add(ResultSetMapper.mapToAutoBid(rs));
                }
            }
        }
        return autoBids;
    }
}
