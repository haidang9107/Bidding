package org.example.server.repository;

import org.example.model.product.*;
import org.example.model.user.Seller;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

	private Connection connection;

	public ProductDao(Connection connection) {
		this.connection = connection;
	}

	public List<Item> getAllProducts() throws SQLException {
		List<Item> products = new ArrayList<>();
		String sql = """
                SELECT p.*,
                       u.user_id, u.username, u.password, u.email, u.phonenumber, u.gender, u.avt, u.balance, u.created_at AS user_created_at
                FROM products p
                JOIN users u ON p.seller_id = u.user_id
                """;

		try (Statement stmt = connection.createStatement();
			 ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				products.add(mapResultSetToItem(rs));
			}
		}
		return products;
	}

	public Item getProductById(String productId) throws SQLException {
		String sql = """
                SELECT p.*,
                       u.user_id, u.username, u.password, u.email, u.phonenumber, u.gender, u.avt, u.balance, u.created_at AS user_created_at
                FROM products p
                JOIN users u ON p.seller_id = u.user_id
                WHERE p.product_id = ?
                """;

		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, productId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return mapResultSetToItem(rs);
				}
			}
		}
		return null;
	}

	private Item mapResultSetToItem(ResultSet rs) throws SQLException {
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

		String category = rs.getString("category");
		String productId = rs.getString("product_id");
		String productName = rs.getString("product_name");
		String description = rs.getString("description");
		double startingPrice = rs.getDouble("starting_price");
		double stepPrice = rs.getDouble("step_price");
		String status = rs.getString("status");
		Timestamp createdAt = rs.getTimestamp("created_at");

		if (category.equalsIgnoreCase("Electronics")) {
			return new Electronics(productId, productName, description, startingPrice, stepPrice, seller, status, createdAt,
					rs.getString("brand"), rs.getInt("warranty_months"));
		} else if (category.equalsIgnoreCase("Art")) {
			return new Art(productId, productName, description, startingPrice, stepPrice, seller, status, createdAt,
					rs.getString("artist"), rs.getString("art_type"));
		} else if (category.equalsIgnoreCase("Vehicle")) {
			return new Vehicle(productId, productName, description, startingPrice, stepPrice, seller, status, createdAt,
					rs.getString("brand"), rs.getString("model"), rs.getInt("manufacture_year"));
		}
		return null;
	}

	public void insertProduct(Item item) throws SQLException {
		String sql = """
                INSERT INTO products(
                    product_id, product_name, description, starting_price, step_price, seller_id, category, status,
                    brand, warranty_months, artist, art_type, model, manufacture_year, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, item.getProductId());
			ps.setString(2, item.getProductName());
			ps.setString(3, item.getDescription());
			ps.setDouble(4, item.getStartingPrice());
			ps.setDouble(5, item.getStepPrice());
			ps.setString(6, item.getSeller().getUserId());

			if (item instanceof Electronics e) {
				ps.setString(7, "Electronics");
				ps.setString(8, item.getStatus());
				ps.setString(9, e.getBrand());
				ps.setInt(10, e.getWarrantyMonths());
				ps.setNull(11, Types.VARCHAR);
				ps.setNull(12, Types.VARCHAR);
				ps.setNull(13, Types.VARCHAR);
				ps.setNull(14, Types.INTEGER);
			} else if (item instanceof Art a) {
				ps.setString(7, "Art");
				ps.setString(8, item.getStatus());
				ps.setNull(9, Types.VARCHAR);
				ps.setNull(10, Types.INTEGER);
				ps.setString(11, a.getArtist());
				ps.setString(12, a.getArtType());
				ps.setNull(13, Types.VARCHAR);
				ps.setNull(14, Types.INTEGER);
			} else if (item instanceof Vehicle v) {
				ps.setString(7, "Vehicle");
				ps.setString(8, item.getStatus());
				ps.setString(9, v.getBrand());
				ps.setNull(10, Types.INTEGER);
				ps.setNull(11, Types.VARCHAR);
				ps.setNull(12, Types.VARCHAR);
				ps.setString(13, v.getModel());
				ps.setInt(14, v.getManufactureYear());
			}
			ps.setTimestamp(15, item.getCreatedAt());
			ps.executeUpdate();
		}
	}
}
