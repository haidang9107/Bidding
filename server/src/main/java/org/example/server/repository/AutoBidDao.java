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

    public List<AutoBid> findActiveCandidates(Connection connection, int auctionId,
                                              String currentWinnerAccountname,
                                              long minimumNextBid) throws SQLException {
        List<AutoBid> autoBids = new ArrayList<>();
        String sql = """
                SELECT *
                FROM auto_bids
                WHERE auction_id = ?
                  AND active = TRUE
                  AND max_bid >= ?
                  AND bidder_accountname <> ?
                ORDER BY max_bid DESC, updated_at ASC
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setLong(2, minimumNextBid);
            ps.setString(3, currentWinnerAccountname == null ? "" : currentWinnerAccountname);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    autoBids.add(mapRow(rs));
                }
            }
        }
        return autoBids;
    }

    private AutoBid mapRow(ResultSet rs) throws SQLException {
        return new AutoBid(
                rs.getInt("auto_bid_id"),
                rs.getInt("auction_id"),
                rs.getString("bidder_accountname"),
                rs.getLong("max_bid"),
                rs.getLong("increment_amount"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")
        );
    }
}
