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

    /**
     * Initializes the database connection pool.
     * Should be called once during application startup.
     */
    public static void init() {
        if (dataSource != null) return;
        
        try {
            HikariConfig config = new HikariConfig();
            // Use DB_ prefix to match user's current .env file
            config.setJdbcUrl(Config.get("DB_URL"));
            config.setUsername(Config.get("DB_USER"));
            config.setPassword(Config.get("DB_PASSWORD"));
            config.setDriverClassName(Config.get("DB_DRIVER"));

            // Performance & Encoding properties
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            // Pool configuration
            config.setMaximumPoolSize(Config.getInt("DB_MAX_POOL_SIZE"));
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(20000);
            config.setMaxLifetime(1800000);

            dataSource = new HikariDataSource(config);
            FileLogger.info("Database connection pool initialized successfully using DB settings from .env.");
        } catch (Exception e) {
            FileLogger.error("Critical: Failed to initialize database pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Checks if the database is currently reachable.
     */
    public static boolean isConnected() {
        if (dataSource == null) {
            init(); // Lazy init if needed, though init() should be called at startup
        }
        if (dataSource == null || dataSource.isClosed()) return false;
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Gets a connection from the connection pool.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            init();
        }
        return dataSource.getConnection();
    }

    /**
     * Closes the database connection pool.
     */
    public static void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            FileLogger.info("Database connection pool closed.");
        }
    }
}
