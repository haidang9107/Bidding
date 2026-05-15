package org.example.server.repository;

import org.example.server.model.Item;
import org.example.server.config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    /**
     * Thêm một sản phẩm mới vào hệ thống (Đăng bán)
     */
    public void insertProduct(Item item) throws SQLException {
        String sql = "INSERT INTO products (product_id, product_name, description, starting_price, " +
                "step_price, seller_id, category, status, brand, warranty_months, " +
                "artist, art_type, model, manufacture_year) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getProductId());
            pstmt.setString(2, item.getProductName());
            pstmt.setString(3, item.getDescription());
            pstmt.setBigDecimal(4, item.getStartingPrice());
            pstmt.setBigDecimal(5, item.getStepPrice());
            pstmt.setString(6, item.getSellerId());
            pstmt.setString(7, item.getCategory());
            pstmt.setString(8, item.getStatus());
            pstmt.setString(9, item.getBrand());
            pstmt.setInt(10, item.getWarrantyMonths());
            pstmt.setString(11, item.getArtist());
            pstmt.setString(12, item.getArtType());
            pstmt.setString(13, item.getModel());
            pstmt.setInt(14, item.getManufactureYear());

            pstmt.executeUpdate();
        }
    }

    /**
     * Lấy danh sách sản phẩm theo Category (VD: Chỉ lấy ART hoặc ELECTRONICS)
     */
    public List<Item> getProductsByCategory(String category) throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE category = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        }
        return items;
    }

    /**
     * Lấy thông tin chi tiết một sản phẩm theo ID
     */
    public Item getProductById(String id) throws SQLException {
        String sql = "SELECT * FROM products WHERE product_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToItem(rs);
                }
            }
        }
        return null;
    }

    /**
     * Cập nhật trạng thái sản phẩm (VD: Từ 'ACTIVE' sang 'SOLD')
     */
    public void updateProductStatus(String productId, String newStatus) throws SQLException {
        String sql = "UPDATE products SET status = ? WHERE product_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setString(2, productId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Hàm phụ trợ ánh xạ dữ liệu từ ResultSet sang Object Item
     */
    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setProductId(rs.getString("product_id"));
        item.setProductName(rs.getString("product_name"));
        item.setDescription(rs.getString("description"));
        item.setStartingPrice(rs.getBigDecimal("starting_price"));
        item.setStepPrice(rs.getBigDecimal("step_price"));
        item.setSellerId(rs.getString("seller_id"));
        item.setCategory(rs.getString("category"));
        item.setStatus(rs.getString("status"));
        item.setBrand(rs.getString("brand"));
        item.setWarrantyMonths(rs.getInt("warranty_months"));
        item.setArtist(rs.getString("artist"));
        item.setArtType(rs.getString("art_type"));
        item.setModel(rs.getString("model"));
        item.setManufactureYear(rs.getInt("manufacture_year"));
        item.setCreatedAt(rs.getTimestamp("created_at"));
        return item;
    }
}