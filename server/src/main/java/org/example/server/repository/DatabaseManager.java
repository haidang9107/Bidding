package org.example.server.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.util.Config;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages database connections for the application using HikariCP Connection Pool.
 */
public class DatabaseManager {
    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(Config.get("BID_DB_URL"));
            config.setUsername(Config.get("BID_DB_USER"));
            config.setPassword(Config.get("BID_DB_PASSWORD"));
            config.setDriverClassName(Config.get("BID_DB_DRIVER"));
            
            // Unicode and encoding properties
            config.addDataSourceProperty("useUnicode", "true");
            config.addDataSourceProperty("characterEncoding", "UTF-8");
            config.addDataSourceProperty("connectionCollation", "utf8mb4_unicode_ci");
            
            // Pool configuration
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(10000);
            config.setMaxLifetime(1800000); // 30 minutes
            
            // Resilience properties
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000);
            
            dataSource = new HikariDataSource(config);
            FileLogger.info("HikariCP Database connection pool established successfully!");
        } catch (Exception e) {
            FileLogger.error("Failed to initialize HikariCP pool", e);
            throw new RuntimeException("Database pool initialization failed", e);
        }
    }

    /**
     * Checks if the database is currently reachable.
     * @return true if connected, false otherwise
     */
    public static boolean isConnected() {
        if (dataSource == null || dataSource.isClosed()) return false;
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Gets a connection from the connection pool.
     * @return the database connection
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            FileLogger.info("Database connection pool closed.");
        }
    }
}
