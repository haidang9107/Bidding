package org.example.server.repository;

import org.example.model.Auction;
import org.example.model.Bid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for bid history.
 */
public class AuctionDao {

    /**
     * Retrieves all auction bid records for a specific auction.
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @return A list of auction bid history records.
     * @throws SQLException If a database error occurs.
     */
    public List<Auction> getAuctionsByProduct(Connection connection, int auctionId) throws SQLException {
        List<Auction> auctions = new ArrayList<>();
        String sql = """
                SELECT bid_id, auction_id, bidder_accountname, bid_amount, bid_time
                FROM bids
                WHERE auction_id = ?
                ORDER BY bid_amount DESC
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    auctions.add(ResultSetMapper.mapToAuction(rs));
                }
            }
        }
        return auctions;
    }

    /**
     * Retrieves a list of bids formatted for display.
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @return A list of bids.
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
     * Retrieves a paged list of bids for an auction.
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @param limit The number of bids to retrieve.
     * @param offset The number of bids to skip.
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
     * Retrieves the total count of bids for a specific auction.
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @return The total count of bids.
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
     * Inserts an auction history record.
     * @param connection The database connection.
     * @param auction The auction record to insert.
     * @return True if successful.
     * @throws SQLException If a database error occurs.
     */
    public boolean insertAuction(Connection connection, Auction auction) throws SQLException {
        return insertBid(connection, auction.getProductId(), auction.getBidderAccountname(),
                auction.getBidAmount(), false);
    }

    /**
     * Inserts a new bid into the database.
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @param bidderAccountname The account name of the bidder.
     * @param bidAmount The amount of the bid.
     * @param autoBid Whether this was an automatic bid.
     * @return True if successful.
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
