package org.example.server.repository;

import org.example.model.enums.Gender;
import org.example.model.enums.UserRole;
import org.example.model.user.*;

import java.sql.*;

/**
 * Data Access Object for managing user-related database operations.
 */
public class UserDao {

    private Connection connection;

    public UserDao(Connection connection) {
        this.connection = connection;
    }

    /**
     * Finds a user by their username.
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
     * Finds a user by their ID.
     */
    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Maps a result set row to a User object.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        int roleValue = rs.getInt("role");
        UserRole role = UserRole.fromInt(roleValue);
        
        int userId = rs.getInt("user_id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String email = rs.getString("email");
        String phoneNumber = rs.getString("phonenumber");
        Gender gender = Gender.fromInt(rs.getInt("gender"));
        String avt = rs.getString("avt");
        long balance = rs.getLong("balance");
        long blockedBalance = rs.getLong("blocked_balance");
        Timestamp createdAt = rs.getTimestamp("created_at");

        if (role == UserRole.ADMIN) {
            return new Admin(userId, username, password, email, phoneNumber, gender, avt, balance, blockedBalance, createdAt);
        } else {
            return new Member(userId, username, password, email, phoneNumber, gender, avt, balance, blockedBalance, createdAt);
        }
    }

    /**
     * Creates a new user in the database.
     */
    public boolean createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, email, phonenumber, gender, avt, balance, blocked_balance, role) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getPhoneNumber());
            pstmt.setInt(5, user.getGender().getValue());
            pstmt.setString(6, user.getAvt());
            pstmt.setLong(7, user.getBalance());
            pstmt.setLong(8, user.getBlockedBalance());
            pstmt.setInt(9, user.getRole().getValue());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setUserId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Updates user's balances.
     */
    public boolean updateBalance(int userId, long balance, long blockedBalance) throws SQLException {
        String sql = "UPDATE users SET balance = ?, blocked_balance = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, balance);
            pstmt.setLong(2, blockedBalance);
            pstmt.setInt(3, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Atomically adds or subtracts from the blocked balance.
     */
    public boolean addBlockedBalance(int userId, long amount) throws SQLException {
        String sql = "UPDATE users SET blocked_balance = blocked_balance + ? WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, amount);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }
}
