package org.example.server.repository;

import org.example.model.Bid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the {@code bids} table (individual bid records placed
 * during an auction).
 */
public class BidDao {

    /**
     * Retrieves a list of bids for an auction (used for client-facing display).
     * @param connection The database connection.
     * @param auctionId  The auction ID.
     * @return A list of bids ordered by amount descending.
     * @throws SQLException If a database error occurs.
     */
    public List<Bid> getBidsForDisplay(Connection connection, int auctionId) throws SQLException {
        List<Bid> bids = new ArrayList<>();
        String sql = """
                SELECT auction_id, bidder_accountname, bid_amount, bid_time
                FROM bids
                WHERE auction_id = ?
                ORDER BY bid_amount DESC
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bids.add(ResultSetMapper.mapToBid(rs));
                }
            }
        }
        return bids;
    }

    /**
     * Retrieves a paged list of bids for an auction, ordered by bid time descending.
     * @param connection The database connection.
     * @param auctionId  The auction ID.
     * @param limit      The maximum number of rows.
     * @param offset     The number of rows to skip.
     * @return A list of bids.
     * @throws SQLException If a database error occurs.
     */
    public List<Bid> getBidsPaged(Connection connection, int auctionId, int limit, int offset) throws SQLException {
        List<Bid> bids = new ArrayList<>();
        String sql = """
                SELECT auction_id, bidder_accountname, bid_amount, bid_time
                FROM bids
                WHERE auction_id = ?
                ORDER BY bid_time DESC
                LIMIT ? OFFSET ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bids.add(ResultSetMapper.mapToBid(rs));
                }
            }
        }
        return bids;
    }

    /**
     * Returns the total number of bids for an auction.
     * @param connection The database connection.
     * @param auctionId  The auction ID.
     * @return The total count.
     * @throws SQLException If a database error occurs.
     */
    public long getTotalBidsCount(Connection connection, int auctionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bids WHERE auction_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    /**
     * Inserts a new bid record.
     * @param connection         The database connection.
     * @param auctionId          The auction ID.
     * @param bidderAccountname  The bidder's account name.
     * @param bidAmount          The bid amount.
     * @param autoBid            True if the bid was placed via auto-bidding.
     * @return True if a row was inserted.
     * @throws SQLException If a database error occurs.
     */
    public boolean insertBid(Connection connection, int auctionId, String bidderAccountname,
                             long bidAmount, boolean autoBid) throws SQLException {
        String sql = """
                INSERT INTO bids(auction_id, bidder_accountname, bid_amount, is_auto_bid)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, auctionId);
            ps.setString(2, bidderAccountname);
            ps.setLong(3, bidAmount);
            ps.setBoolean(4, autoBid);
            return ps.executeUpdate() > 0;
        }
    }
}
