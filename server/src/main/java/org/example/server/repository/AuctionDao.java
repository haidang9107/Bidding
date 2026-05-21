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
                    auctions.add(new Auction(
                            rs.getInt("bid_id"),
                            rs.getInt("auction_id"),
                            rs.getString("bidder_accountname"),
                            rs.getLong("bid_amount"),
                            rs.getTimestamp("bid_time")
                    ));
                }
            }
        }
        return auctions;
    }

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
                    bids.add(new Bid(
                            rs.getInt("auction_id"),
                            rs.getString("bidder_accountname"),
                            rs.getLong("bid_amount"),
                            rs.getTimestamp("bid_time")
                    ));
                }
            }
        }
        return bids;
    }

    public boolean insertAuction(Connection connection, Auction auction) throws SQLException {
        return insertBid(connection, auction.getProductId(), auction.getBidderAccountname(),
                auction.getBidAmount(), false);
    }

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
