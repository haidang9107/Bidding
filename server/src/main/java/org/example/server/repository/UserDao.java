package org.example.server.repository;

import org.example.model.user.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing user-related database operations.
 * Simplified: Uses accountname as Primary Key.
 */
public class UserDao {

    /**
     * Constructs a new UserDao.
     */
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
                    return ResultSetMapper.mapToUser(rs);
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
                    return ResultSetMapper.mapToUser(rs);
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
     * Creates a new user in the database.
     */
    public boolean createUser(Connection connection, User user) throws SQLException {
        String sql = "INSERT INTO users (accountname, fullname, password, email, avt, balance, blocked_balance, role, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getAccountname());
            pstmt.setString(2, user.getFullname() == null ? user.getAccountname() : user.getFullname());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getAvt());
            
            if (user instanceof Member member) {
                pstmt.setLong(6, member.getBalance());
                pstmt.setLong(7, member.getBlockedBalance());
            } else {
                pstmt.setLong(6, 0);
                pstmt.setLong(7, 0);
            }
            
            pstmt.setInt(8, user.getRole().getValue());
            pstmt.setInt(9, user.getStatus());
            
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
     * Updates the user's email.
     */
    public boolean updateEmail(Connection connection, String accountname, String email) throws SQLException {
        String sql = "UPDATE users SET email = ? WHERE accountname = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
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
     * Retrieves a page of users from the database.
     */
    public List<User> getUsersPaged(Connection connection, int limit, int offset) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users LIMIT ? OFFSET ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(ResultSetMapper.mapToUser(rs));
                }
            }
        }
        return users;
    }

    /**
     * Retrieves the total count of users in the database.
     */
    public long getTotalUsersCount(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
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
                users.add(ResultSetMapper.mapToUser(rs));
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
