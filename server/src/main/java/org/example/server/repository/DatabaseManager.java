package org.example.server.repository;

import org.example.util.FileLogger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages database connections for the application.
 */
public class DatabaseManager {
    // Connection information from application.yml (or hardcoded for demo)
    private static final String URL = "jdbc:mysql://localhost:3306/MySQL-DB?createDatabaseIfNotExist=true";
    private static final String USER = "HaiDang91";
    private static final String PASSWORD = "9107";

    private static Connection connection;

    /**
     * Gets a singleton connection to the database.
     *
     * @return the database connection
     * @throws SQLException if a database access error occurs
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Ensure driver is loaded
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                FileLogger.info("Kết nối Database MySQL thành công!");
            } catch (ClassNotFoundException e) {
                FileLogger.error("Không tìm thấy Driver MySQL", e);
                throw new SQLException(">>> Không tìm thấy Driver MySQL: " + e.getMessage());
            } catch (SQLException e) {
                FileLogger.error("Lỗi kết nối Database", e);
                throw e;
            }
        }
        return connection;
    }

    /**
     * Closes the database connection if it is open.
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                FileLogger.info("Đã đóng kết nối Database.");
            }
        } catch (SQLException e) {
            FileLogger.error("Lỗi khi đóng kết nối Database", e);
        }
    }
}
