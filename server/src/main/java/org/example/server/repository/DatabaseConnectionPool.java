package org.example.server.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.util.Config;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Replaces the static DatabaseManager singleton.
 * An instance is created once in ServerApp and injected wherever needed.
 */
public class DatabaseConnectionPool {

    private final HikariDataSource dataSource;

    /**
     * Initializes the database connection pool with settings from configuration.
     */
    public DatabaseConnectionPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(Config.get("DB_URL"));
        config.setUsername(Config.get("DB_USER"));
        config.setPassword(Config.get("DB_PASSWORD"));
        config.setDriverClassName(Config.get("DB_DRIVER"));

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        config.setMaximumPoolSize(Config.getInt("DB_MAX_POOL_SIZE"));
        config.setMinimumIdle(2);
        config.setIdleTimeout(30_000);
        config.setConnectionTimeout(20_000);
        config.setMaxLifetime(1_800_000);

        this.dataSource = new HikariDataSource(config);
        FileLogger.info("Database connection pool initialized (Max Size: " + Config.getInt("DB_MAX_POOL_SIZE") + ")");
    }

    /**
     * Obtains a connection from the pool.
     * @return A database connection.
     * @throws SQLException If a connection cannot be obtained.
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Checks if the connection pool is healthy.
     * @return True if a valid connection can be obtained, false otherwise.
     */
    public boolean isHealthy() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Closes the connection pool and releases all resources.
     */
    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
            FileLogger.info("Database connection pool closed.");
        }
    }
}
