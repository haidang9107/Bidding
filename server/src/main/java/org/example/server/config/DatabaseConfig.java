package org.example.server.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    // Thông tin cấu hình từ database của bạn
    private static final String URL = "jdbc:mysql://localhost:3306/bidding";
    private static final String USER = "MINHANH";
    private static final String PASSWORD = "123456";

    /**
     * Phương thức cung cấp kết nối đến MySQL.
     * Các lớp DAO sẽ gọi hàm này để thực hiện truy vấn.
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Nạp Driver (không bắt buộc với các bản Java mới nhưng nên có để ổn định)
            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            // System.out.println("Kết nối Database thành công!"); // Có thể tắt đi khi chạy thực tế
            return conn;
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy MySQL Driver!");
            throw new SQLException(e);
        } catch (SQLException e) {
            System.err.println("Kết nối thất bại! Kiểm tra lại URL, User hoặc Password.");
            throw e;
        }
    }
}