package org.example.server.repository;

import org.example.util.Config;
import org.example.util.FileLogger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages database connections for the application using centralized Config.
 */
public class DatabaseManager {
    private static Connection connection;

    /**
     * Gets a singleton connection to the database.
     * @return the database connection
     * @throws SQLException if a database access error occurs
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = Config.get("DB_URL");
            String user = Config.get("DB_USER");
            String password = Config.get("DB_PASSWORD");

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(url, user, password);
                FileLogger.info("Database connection established successfully!");
            } catch (ClassNotFoundException e) {
                FileLogger.error("MySQL Driver not found", e);
                throw new SQLException("MySQL Driver not found: " + e.getMessage());
            } catch (SQLException e) {
                FileLogger.error("Database connection failed", e);
                throw e;
            }
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                FileLogger.info("Database connection closed.");
            }
        } catch (SQLException e) {
            FileLogger.error("Error closing database connection", e);
        }
    }
}
