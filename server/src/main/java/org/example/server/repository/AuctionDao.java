package org.example.server.repository;

import org.example.model.Auction;
import org.example.model.Bid;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing auction-related database operations.
 */
public class AuctionDao {

    public AuctionDao() {
    }

    /**
     * Retrieves all auction records (bids) for a specific product.
     */
    public List<Auction> getAuctionsByProduct(Connection connection, int productId) throws SQLException {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE product_id = ? ORDER BY bid_amount DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    auctions.add(new Auction(
                            rs.getInt("auction_id"),
                            rs.getInt("product_id"),
                            rs.getInt("bidder_id"),
                            rs.getLong("bid_amount"),
                            rs.getTimestamp("bid_time")
                    ));
                }
            }
        }
        return auctions;
    }

    /**
     * Retrieves all bids for display, including bidder usernames.
     */
    public List<Bid> getBidsForDisplay(Connection connection, int productId) throws SQLException {
        List<Bid> bids = new ArrayList<>();
        String sql = """
                SELECT a.product_id, a.bidder_id, u.username, a.bid_amount, a.bid_time
                FROM auctions a
                JOIN users u ON a.bidder_id = u.user_id
                WHERE a.product_id = ?
                ORDER BY a.bid_amount DESC
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bids.add(new Bid(
                            rs.getInt("product_id"),
                            rs.getInt("bidder_id"),
                            rs.getString("username"),
                            rs.getLong("bid_amount"),
                            rs.getTimestamp("bid_time")
                    ));
                }
            }
        }
        return bids;
    }

    /**
     * Inserts a new auction (bid) into the database.
     */
    public boolean insertAuction(Connection connection, Auction auction) throws SQLException {
        String sql = "INSERT INTO auctions(product_id, bidder_id, bid_amount) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, auction.getProductId());
            ps.setInt(2, auction.getBidderId());
            ps.setLong(3, auction.getBidAmount());
            
            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        auction.setAuctionId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }
}
