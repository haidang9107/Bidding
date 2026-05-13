package org.example.server.repository;

import org.example.model.user.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

	// =========================
	// Connection tới database
	// =========================
	private Connection connection;

	// =========================
	// Constructor
	// =========================
	public UserDao(Connection connection) {
		this.connection = connection;
	}

	// =========================
	// Lấy user theo username
	// =========================
	public User findByUsername(String username) throws SQLException {
		String sql = "SELECT * FROM users WHERE username = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setString(1, username);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return mapResultSetToUser(rs);
				}
			}
		}
		return null;
	}

	private User mapResultSetToUser(ResultSet rs) throws SQLException {
		String role = rs.getString("role");
		if (role.equalsIgnoreCase("ADMIN")) {
			return new Admin(
					rs.getString("user_id"),
					rs.getString("username"),
					rs.getString("password"),
					rs.getString("email"),
					rs.getString("phonenumber"),
					rs.getString("gender"),
					rs.getString("avt"),
					rs.getDouble("balance"),
					rs.getTimestamp("created_at")
			);
		} else if (role.equalsIgnoreCase("SELLER")) {
			return new Seller(
					rs.getString("user_id"),
					rs.getString("username"),
					rs.getString("password"),
					rs.getString("email"),
					rs.getString("phonenumber"),
					rs.getString("gender"),
					rs.getString("avt"),
					rs.getDouble("balance"),
					rs.getTimestamp("created_at")
			);
		} else {
			return new Bidder(
					rs.getString("user_id"),
					rs.getString("username"),
					rs.getString("password"),
					rs.getString("email"),
					rs.getString("phonenumber"),
					rs.getString("gender"),
					rs.getString("avt"),
					rs.getDouble("balance"),
					rs.getTimestamp("created_at")
			);
		}
	}

	// =========================
	// Tạo user mới (cho Register)
	// =========================
	public boolean createUser(User user, String role) throws SQLException {
		String sql = "INSERT INTO users (user_id, username, password, email, phonenumber, gender, avt, balance, role, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setString(1, user.getUserId());
			pstmt.setString(2, user.getUsername());
			pstmt.setString(3, user.getPassword());
			pstmt.setString(4, user.getEmail());
			pstmt.setString(5, user.getPhoneNumber());
			pstmt.setString(6, user.getGender());
			pstmt.setString(7, user.getAvt());
			pstmt.setDouble(8, user.getBalance());
			pstmt.setString(9, role);
			pstmt.setTimestamp(10, user.getCreatedAt());
			return pstmt.executeUpdate() > 0;
		}
	}
}