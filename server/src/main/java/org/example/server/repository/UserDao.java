package org.example.server.repository;

import org.example.model.user.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing user-related database operations.
 */
public class UserDao {

	// =========================
	// Connection tới database
	// =========================
	private Connection connection;

	/**
	 * Constructor for UserDao.
	 *
	 * @param connection the database connection to use
	 */
	public UserDao(Connection connection) {
		this.connection = connection;
	}

	/**
	 * Finds a user by their username.
	 *
	 * @param username the username to search for
	 * @return the user if found, or null otherwise
	 * @throws SQLException if a database access error occurs
	 */
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

	/**
	 * Maps a result set row to a User object (Admin, Seller, or Bidder).
	 *
	 * @param rs the result set
	 * @return the mapped User object
	 * @throws SQLException if a database access error occurs
	 */
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

	/**
	 * Creates a new user in the database.
	 *
	 * @param user the user object to insert
	 * @param role the role of the user (e.g., ADMIN, SELLER, BIDDER)
	 * @return true if the user was created successfully, false otherwise
	 * @throws SQLException if a database access error occurs
	 */
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
