package org.example.server.repository;

import org.example.model.enums.AuctionStatus;
import org.example.model.enums.ItemCategory;
import org.example.model.product.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing product-related database operations.
 */
public class ProductDao {

    public ProductDao() {
    }

    /**
     * Retrieves all products from the database.
     */
    public List<Item> getAllProducts(Connection connection) throws SQLException {
        List<Item> products = new ArrayList<>();
        String sql = "SELECT * FROM products";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapResultSetToItem(rs));
            }
        }
        return products;
    }

    /**
     * Retrieves a product by its ID.
     */
    public Item getProductById(Connection connection, int productId) throws SQLException {
        String sql = "SELECT * FROM products WHERE product_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToItem(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves a product by its ID and locks the row for the transaction.
     */
    public Item getProductForUpdate(Connection connection, int productId) throws SQLException {
        String sql = "SELECT * FROM products WHERE product_id = ? FOR UPDATE";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToItem(rs);
                }
            }
        }
        return null;
    }

    /**
     * Checks if a user is the current leader in any active (RUNNING) auction.
     */
    public boolean isUserLeadingAnyAuction(Connection connection, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE winner_id = ? AND status = 1"; // 1: RUNNING
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Maps a result set row to an Item object.
     */
    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        int productId = rs.getInt("product_id");
        String productName = rs.getString("product_name");
        String description = rs.getString("description");
        long startingPrice = rs.getLong("starting_price");
        long currentPrice = rs.getLong("current_price");
        long stepPrice = rs.getLong("step_price");
        int sellerId = rs.getInt("seller_id");
        
        Integer winnerId = rs.getInt("winner_id");
        if (rs.wasNull()) {
            winnerId = null;
        }

        ItemCategory category = ItemCategory.fromInt(rs.getInt("category"));
        AuctionStatus status = AuctionStatus.fromInt(rs.getInt("status"));
        
        Timestamp startTime = rs.getTimestamp("start_time");
        Timestamp endTime = rs.getTimestamp("end_time");
        int version = rs.getInt("version");
        Timestamp createdAt = rs.getTimestamp("created_at");

        if (category == ItemCategory.ELECTRONICS) {
            return new Electronics(productId, productName, description, startingPrice, currentPrice, stepPrice, 
                    sellerId, winnerId, status, startTime, endTime, version, createdAt,
                    rs.getString("brand"), rs.getInt("warranty_months"));
        } else if (category == ItemCategory.ART) {
            return new Art(productId, productName, description, startingPrice, currentPrice, stepPrice, 
                    sellerId, winnerId, status, startTime, endTime, version, createdAt,
                    rs.getString("artist"), rs.getString("art_type"));
        } else {
            return new Vehicle(productId, productName, description, startingPrice, currentPrice, stepPrice, 
                    sellerId, winnerId, status, startTime, endTime, version, createdAt,
                    rs.getString("brand"), rs.getString("model"), rs.getInt("manufacture_year"));
        }
    }

    /**
     * Inserts a new product into the database.
     */
    public boolean insertProduct(Connection connection, Item item) throws SQLException {
        String sql = """
                INSERT INTO products(
                    product_name, description, starting_price, current_price, step_price, 
                    seller_id, category, status, start_time, end_time, brand, 
                    warranty_months, artist, art_type, model, manufacture_year
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getProductName());
            ps.setString(2, item.getDescription());
            ps.setLong(3, item.getStartingPrice());
            ps.setLong(4, item.getCurrentPrice());
            ps.setLong(5, item.getStepPrice());
            ps.setInt(6, item.getSellerId());
            ps.setInt(7, item.getCategory().getValue());
            ps.setInt(8, item.getStatus().getValue());
            ps.setTimestamp(9, item.getStartTime());
            ps.setTimestamp(10, item.getEndTime());

            if (item instanceof Electronics e) {
                ps.setString(11, e.getBrand());
                ps.setInt(12, e.getWarrantyMonths());
                ps.setNull(13, Types.VARCHAR);
                ps.setNull(14, Types.VARCHAR);
                ps.setNull(15, Types.VARCHAR);
                ps.setNull(16, Types.INTEGER);
            } else if (item instanceof Art a) {
                ps.setNull(11, Types.VARCHAR);
                ps.setNull(12, Types.INTEGER);
                ps.setString(13, a.getArtist());
                ps.setString(14, a.getArtType());
                ps.setNull(15, Types.VARCHAR);
                ps.setNull(16, Types.INTEGER);
            } else if (item instanceof Vehicle v) {
                ps.setString(11, v.getBrand());
                ps.setNull(12, Types.INTEGER);
                ps.setNull(13, Types.VARCHAR);
                ps.setNull(14, Types.VARCHAR);
                ps.setString(15, v.getModel());
                ps.setInt(16, v.getManufactureYear());
            }

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        item.setProductId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the current price and winner of a product using Optimistic Locking.
     */
    public boolean updateBid(Connection connection, int productId, long newPrice, int bidderId, int oldVersion) throws SQLException {
        String sql = "UPDATE products SET current_price = ?, winner_id = ?, version = version + 1 WHERE product_id = ? AND version = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, newPrice);
            ps.setInt(2, bidderId);
            ps.setInt(3, productId);
            ps.setInt(4, oldVersion);
            return ps.executeUpdate() > 0;
        }
    }
}
