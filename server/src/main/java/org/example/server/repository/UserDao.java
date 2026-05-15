package org.example.server.repository;

import org.example.server.model.User;
import org.example.server.config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    /**
     * Thêm người dùng mới (Dùng cho chức năng Đăng ký)
     */
    public void insertUser(User user) throws SQLException {
        String sql = "INSERT INTO users (user_id, username, password, email, phonenumber, gender, role, balance) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUserId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword()); // Lưu ý: thực tế nên hash password trước khi lưu
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getPhoneNumber());
            pstmt.setString(6, user.getGender());
            pstmt.setString(7, user.getRole());
            pstmt.setBigDecimal(8, user.getBalance());

            pstmt.executeUpdate();
        }
    }

    /**
     * Tìm người dùng theo Email (Dùng cho chức năng Đăng nhập)
     */
    public User getUserByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Lấy danh sách toàn bộ người dùng (Dùng cho Admin quản lý)
     */
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    /**
     * Cập nhật số dư tài khoản (Dùng khi nạp tiền hoặc trừ tiền đấu giá)
     */
    public void updateBalance(String userId, java.math.BigDecimal newBalance) throws SQLException {
        String sql = "UPDATE users SET balance = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBigDecimal(1, newBalance);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Xóa người dùng theo ID
     */
    public void deleteUser(String userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Hàm phụ trợ ánh xạ dữ liệu từ ResultSet sang Object User
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getString("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setPhoneNumber(rs.getString("phonenumber"));
        user.setGender(rs.getString("gender"));
        user.setAvt(rs.getString("avt"));
        user.setBalance(rs.getBigDecimal("balance"));
        user.setRole(rs.getString("role"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}