package org.example.server.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    // Thông tin kết nối lấy từ application.yml (hoặc cấu hình cứng cho demo)
    private static final String URL = "jdbc:postgresql://localhost:7108/PostgresDB";
    private static final String USER = "HaiDang91";
    private static final String PASSWORD = "9107";

    private static Connection connection;

    /**
     * Lấy kết nối duy nhất tới Database (Singleton)
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Đảm bảo Driver đã được load
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println(">>> Kết nối Database thành công!");
            } catch (ClassNotFoundException e) {
                throw new SQLException(">>> Không tìm thấy Driver PostgreSQL: " + e.getMessage());
            }
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println(">>> Đã đóng kết nối Database.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
