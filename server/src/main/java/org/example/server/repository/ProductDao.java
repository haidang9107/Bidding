package org.example.server.repository;

import org.example.server.model.Art;
import org.example.server.model.Electronics;
import org.example.server.model.Item;
import org.example.server.model.Seller;
import org.example.server.model.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    // =========================
    // Connection
    // =========================
    private Connection connection;

    // =========================
    // Constructor
    // =========================
    public ProductDao(Connection connection) {
        this.connection = connection;
    }

    // =========================
    // Lấy toàn bộ products
    // =========================
    public List<Item> getAllProducts() throws SQLException {

        List<Item> products = new ArrayList<>();

        String sql = """
                SELECT p.*,
                       u.user_id,
                       u.username,
                       u.password,
                       u.email,
                       u.phonenumber,
                       u.gender,
                       u.avt,
                       u.balance,
                       u.created_at AS user_created_at
                       
                FROM products p
                JOIN users u
                ON p.seller_id = u.user_id
                """;

        try (
                Statement stmt = connection.createStatement();

                ResultSet rs = stmt.executeQuery(sql)
        ) {

            while (rs.next()) {

                // =========================
                // Tạo Seller object
                // =========================
                Seller seller = new Seller(
                        rs.getString("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("phonenumber"),
                        rs.getString("gender"),
                        rs.getString("avt"),
                        rs.getDouble("balance"),
                        rs.getTimestamp("user_created_at")
                );

                // =========================
                // Đọc category
                // =========================
                String category =
                        rs.getString("category");

                Item item;

                // =========================
                // Mapping category -> object
                // =========================
                if (category.equalsIgnoreCase(
                        "Electronics")) {

                    item = new Electronics(
                            rs.getString("product_id"),
                            rs.getString("product_name"),
                            rs.getString("description"),
                            rs.getDouble("starting_price"),
                            rs.getDouble("step_price"),
                            seller,
                            rs.getString("status"),
                            rs.getTimestamp("created_at")
                    );

                } else if (category.equalsIgnoreCase(
                        "Art")) {

                    item = new Art(
                            rs.getString("product_id"),
                            rs.getString("product_name"),
                            rs.getString("description"),
                            rs.getDouble("starting_price"),
                            rs.getDouble("step_price"),
                            seller,
                            rs.getString("status"),
                            rs.getTimestamp("created_at")
                    );

                } else {

                    item = new Vehicle(
                            rs.getString("product_id"),
                            rs.getString("product_name"),
                            rs.getString("description"),
                            rs.getDouble("starting_price"),
                            rs.getDouble("step_price"),
                            seller,
                            rs.getString("status"),
                            rs.getTimestamp("created_at")
                    );
                }

                products.add(item);
            }
        }

        return products;
    }

    // =========================
    // Tìm product theo ID
    // =========================
    public Item getProductById(String productId)
            throws SQLException {

        String sql = """
                SELECT p.*,
                       u.user_id,
                       u.username,
                       u.password,
                       u.email,
                       u.phonenumber,
                       u.gender,
                       u.avt,
                       u.balance,
                       u.created_at AS user_created_at
                       
                FROM products p
                JOIN users u
                ON p.seller_id = u.user_id
                
                WHERE p.product_id = ?
                """;

        try (
                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setString(1, productId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    Seller seller = new Seller(
                            rs.getString("user_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email"),
                            rs.getString("phonenumber"),
                            rs.getString("gender"),
                            rs.getString("avt"),
                            rs.getDouble("balance"),
                            rs.getTimestamp("user_created_at")
                    );

                    String category =
                            rs.getString("category");

                    if (category.equalsIgnoreCase(
                            "Electronics")) {

                        return new Electronics(
                                rs.getString("product_id"),
                                rs.getString("product_name"),
                                rs.getString("description"),
                                rs.getDouble("starting_price"),
                                rs.getDouble("step_price"),
                                seller,
                                rs.getString("status"),
                                rs.getTimestamp("created_at")
                        );

                    } else if (category.equalsIgnoreCase(
                            "Art")) {

                        return new Art(
                                rs.getString("product_id"),
                                rs.getString("product_name"),
                                rs.getString("description"),
                                rs.getDouble("starting_price"),
                                rs.getDouble("step_price"),
                                seller,
                                rs.getString("status"),
                                rs.getTimestamp("created_at")
                        );

                    } else {

                        return new Vehicle(
                                rs.getString("product_id"),
                                rs.getString("product_name"),
                                rs.getString("description"),
                                rs.getDouble("starting_price"),
                                rs.getDouble("step_price"),
                                seller,
                                rs.getString("status"),
                                rs.getTimestamp("created_at")
                        );
                    }
                }
            }
        }

        return null;
    }

    // =========================
    // Insert product
    // =========================
    public void insertProduct(Item item)
            throws SQLException {

        String sql = """
                INSERT INTO products(
                    product_id,
                    product_name,
                    description,
                    starting_price,
                    step_price,
                    seller_id,
                    category,
                    status,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (
                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setString(1, item.getProductId());

            ps.setString(2, item.getProductName());

            ps.setString(3, item.getDescription());

            ps.setDouble(4, item.getStartingPrice());

            ps.setDouble(5, item.getStepPrice());

            ps.setString(
                    6,
                    item.getSeller().getUserId()
            );

            // =========================
            // Xác định category
            // =========================
            if (item instanceof Electronics) {

                ps.setString(7, "Electronics");

            } else if (item instanceof Art) {

                ps.setString(7, "Art");

            } else {

                ps.setString(7, "Vehicle");
            }

            ps.setString(8, item.getStatus());

            ps.setTimestamp(9, item.getCreatedAt());

            ps.executeUpdate();
        }
    }

    // =========================
    // Xóa product
    // =========================
    public void deleteProduct(String productId)
            throws SQLException {

        String sql =
                "DELETE FROM products WHERE product_id = ?";

        try (
                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setString(1, productId);

            ps.executeUpdate();
        }
    }

    // =========================
    // Update status
    // =========================
    public void updateStatus(
            String productId,
            String status
    ) throws SQLException {

        String sql = """
                UPDATE products
                SET status = ?
                WHERE product_id = ?
                """;

        try (
                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setString(1, status);

            ps.setString(2, productId);

            ps.executeUpdate();
        }
    }
}