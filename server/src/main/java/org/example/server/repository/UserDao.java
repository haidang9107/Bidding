package org.example.server.repository;

import org.example.model.enums.UserRole;
import org.example.model.user.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing user-related database operations.
 * Simplified: Uses accountname as Primary Key.
 */
public class UserDao {

    public UserDao() {
    }

    /**
     * Finds a user by their accountname.
     */
    public User findByAccountname(Connection connection, String accountname) throws SQLException {
        String sql = "SELECT * FROM users WHERE accountname = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, accountname);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a user by their accountname and locks the row for update.
     */
    public User findByAccountnameForUpdate(Connection connection, String accountname) throws SQLException {
        String sql = "SELECT * FROM users WHERE accountname = ? FOR UPDATE";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, accountname);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Atomically adds or subtracts from the balance.
     */
    public boolean addBalance(Connection connection, String accountname, long amount) throws SQLException {
        String sql = "UPDATE users SET balance = balance + ? WHERE accountname = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, amount);
            pstmt.setString(2, accountname);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Maps a result set row to a User object.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        int roleValue = rs.getInt("role");
        UserRole role = UserRole.fromInt(roleValue);
        
        String accountname = rs.getString("accountname");
        String password = rs.getString("password");
        String email = rs.getString("email");
        String avt = rs.getString("avt");
        long balance = rs.getLong("balance");
        long blockedBalance = rs.getLong("blocked_balance");
        int status = rs.getInt("status");

        if (role == UserRole.ADMIN) {
            return new Admin(accountname, password, email, avt, status);
        } else {
            return new Member(accountname, password, email, avt, status, balance, blockedBalance);
        }
    }

    /**
     * Creates a new user in the database.
     */
    public boolean createUser(Connection connection, User user) throws SQLException {
        String sql = "INSERT INTO users (accountname, password, email, avt, balance, blocked_balance, role, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getAccountname());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getAvt());
            
            if (user instanceof Member member) {
                pstmt.setLong(5, member.getBalance());
                pstmt.setLong(6, member.getBlockedBalance());
            } else {
                pstmt.setLong(5, 0);
                pstmt.setLong(6, 0);
            }
            
            pstmt.setInt(7, user.getRole().getValue());
            pstmt.setInt(8, user.getStatus());
            
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates the user's avatar.
     */
    public boolean updateAvatar(Connection connection, String accountname, String avatarPath) throws SQLException {
        String sql = "UPDATE users SET avt = ? WHERE accountname = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, avatarPath);
            pstmt.setString(2, accountname);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates the user's status (e.g., Active/Banned).
     */
    public boolean updateUserStatus(Connection connection, String accountname, int status) throws SQLException {
        String sql = "UPDATE users SET status = ? WHERE accountname = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, status);
            pstmt.setString(2, accountname);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all users from the database.
     */
    public List<User> findAllUsers(Connection connection) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    /**
     * Updates user's balances.
     */
    public boolean updateBalance(Connection connection, String accountname, long balance, long blockedBalance) throws SQLException {
        String sql = "UPDATE users SET balance = ?, blocked_balance = ? WHERE accountname = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, balance);
            pstmt.setLong(2, blockedBalance);
            pstmt.setString(3, accountname);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Atomically adds or subtracts from the blocked balance.
     */
    public boolean addBlockedBalance(Connection connection, String accountname, long amount) throws SQLException {
        String sql = "UPDATE users SET blocked_balance = blocked_balance + ? WHERE accountname = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, amount);
            pstmt.setString(2, accountname);
            return pstmt.executeUpdate() > 0;
        }
    }
}
