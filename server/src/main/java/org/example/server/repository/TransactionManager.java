package org.example.server.repository;

import org.example.server.exception.DatabaseException;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Eliminates the repetitive try/setAutoCommit/commit/rollback/finally boilerplate.
 */
public class TransactionManager {

    /**
     * Functional interface for work that returns a result and may throw SQLException.
     * @param <T> The return type of the work.
     */
    @FunctionalInterface
    public interface TransactionalWork<T> {
        /**
         * Executes the work using the provided connection.
         * @param connection The database connection.
         * @return The result of the work.
         * @throws SQLException If a database error occurs.
         */
        T execute(Connection connection) throws SQLException;
    }

    /**
     * Functional interface for work that doesn't return a result and may throw SQLException.
     */
    @FunctionalInterface
    public interface TransactionalRunnable {
        /**
         * Executes the work using the provided connection.
         * @param connection The database connection.
         * @throws SQLException If a database error occurs.
         */
        void execute(Connection connection) throws SQLException;
    }

    private final DatabaseConnectionPool pool;

    /**
     * Constructs a TransactionManager with the specified connection pool.
     * @param pool The connection pool to use.
     */
    public TransactionManager(DatabaseConnectionPool pool) {
        this.pool = pool;
    }

    /**
     * Opens a connection, runs work inside a transaction, commits on success,
     * rolls back and rethrows on any failure.
     * @param <T> The return type of the work.
     * @param work The work to execute.
     * @return The result of the work.
     */
    public <T> T execute(TransactionalWork<T> work) {
        try (Connection conn = pool.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.execute(conn);
                conn.commit();
                return result;
            } catch (SQLException e) {
                rollbackQuietly(conn);
                throw new DatabaseException("Transaction failed: " + e.getMessage(), e);
            } catch (Exception e) {
                rollbackQuietly(conn);
                if (e instanceof RuntimeException re) throw re;
                throw new DatabaseException("Unexpected error during transaction", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not obtain connection: " + e.getMessage(), e);
        }
    }

    /**
     * Executes the work inside a transaction without returning a result.
     * @param work The work to execute.
     */
    public void run(TransactionalRunnable work) {
        execute(conn -> {
            work.execute(conn);
            return null;
        });
    }

    /**
     * Convenience for read-only queries that don't need a transaction.
     * @param <T> The return type.
     * @param work The work to execute.
     * @return The result.
     */
    public <T> T query(TransactionalWork<T> work) {
        try (Connection conn = pool.getConnection()) {
            return work.execute(conn);
        } catch (SQLException e) {
            throw new DatabaseException("Query failed: " + e.getMessage(), e);
        }
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            FileLogger.error("Rollback failed", ex);
        }
    }
}