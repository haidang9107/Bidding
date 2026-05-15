package org.example.server.test;

import org.example.server.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class MyServerApplication {

    public static void main(String[] args) {
        System.out.println("--- ĐANG KIỂM TRA KẾT NỐI DATABASE (JAVA THUẦN) ---");

        try (Connection conn = DatabaseConfig.getConnection()) {
            if (conn != null) {
                System.out.println("✅ Kết nối thành công!");
                System.out.println("Thông tin Database: " + conn.getMetaData().getDatabaseProductVersion());

                // Chạy thử một câu lệnh truy vấn đơn giản để kiểm tra bảng users
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM users");

                if (rs.next()) {
                    System.out.println("Số lượng người dùng hiện có trong DB: " + rs.getInt("total"));
                }

                rs.close();
                stmt.close();
            }
        } catch (Exception e) {
            System.err.println("❌ Kết nối thất bại!");
            System.err.println("Lỗi cụ thể: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("---------------------------------------------------");
    }
}