package org.example.server.repository;

import org.example.server.model.Auction;
import org.example.server.config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDao {

    /**
     * Thực hiện một lượt trả giá mới
     */
    public void insertBid(Auction auction) throws SQLException {
        String sql = "INSERT INTO auctions (auction_id, product_id, bidder_id, bid_amount) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, auction.getAuctionId());
            pstmt.setString(2, auction.getProductId());
            pstmt.setString(3, auction.getBidderId());
            pstmt.setBigDecimal(4, auction.getBidAmount());

            pstmt.executeUpdate();
        }
    }

    /**
     * Lấy lượt trả giá cao nhất hiện tại của một sản phẩm
     */
    public Auction getHighestBid(String productId) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE product_id = ? ORDER BY bid_amount DESC LIMIT 1";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAuction(rs);
                }
            }
        }
        return null;
    }

    /**
     * Lấy lịch sử tất cả các lượt trả giá của một sản phẩm (từ mới nhất đến cũ nhất)
     */
    public List<Auction> getBidHistoryByProduct(String productId) throws SQLException {
        List<Auction> history = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE product_id = ? ORDER BY bid_time DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    history.add(mapResultSetToAuction(rs));
                }
            }
        }
        return history;
    }

    /**
     * Lấy danh sách các sản phẩm mà một người dùng cụ thể đã tham gia đấu giá
     */
    public List<String> getParticipatedProductIds(String bidderId) throws SQLException {
        List<String> productIds = new ArrayList<>();
        String sql = "SELECT DISTINCT product_id FROM auctions WHERE bidder_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bidderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    productIds.add(rs.getString("product_id"));
                }
            }
        }
        return productIds;
    }

    /**
     * Hàm phụ trợ ánh xạ dữ liệu từ ResultSet sang Object Auction
     */
    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setAuctionId(rs.getString("auction_id"));
        auction.setProductId(rs.getString("product_id"));
        auction.setBidderId(rs.getString("bidder_id"));
        auction.setBidAmount(rs.getBigDecimal("bid_amount"));
        // bid_time trong DB được map vào trường createdAt của Entity
        auction.setCreatedAt(rs.getTimestamp("bid_time"));
        return auction;
    }
}