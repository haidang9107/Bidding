package org.example.server.repository;

import org.example.model.Transaction;
import org.example.model.enums.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for money movement and asset ownership audit records.
 */
public class TransactionDao {

    /**
     * Inserts a new financial transaction record.
     * @param connection The database connection.
     * @param senderAccountname The sender's account name.
     * @param receiverAccountname The receiver's account name.
     * @param type The transaction type.
     * @param productId Optional product ID related to the transaction.
     * @param amount The transaction amount.
     * @param auctionId Optional auction ID related to the transaction.
     * @param description A brief description of the transaction.
     * @return True if successful.
     * @throws SQLException If a database error occurs.
     */
    public boolean insertTransaction(Connection connection, String senderAccountname,
                                     String receiverAccountname, TransactionType type, Integer productId,
                                     long amount, Integer auctionId, String description)
            throws SQLException {
        String sql = """
                INSERT INTO transactions(
                    sender_accountname, receiver_accountname, type, product_id,
                    amount, auction_id, description
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (senderAccountname == null) {
                ps.setNull(1, Types.VARCHAR);
            } else {
                ps.setString(1, senderAccountname);
            }
            if (receiverAccountname == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, receiverAccountname);
            }
            ps.setInt(3, type.getValue());
            if (productId == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, productId);
            }
            ps.setLong(5, amount);
            if (auctionId == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, auctionId);
            }
            ps.setString(7, description);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all transactions involving a specific user.
     * @param connection The database connection.
     * @param accountname The account name.
     * @return A list of transactions.
     * @throws SQLException If a database error occurs.
     */
    public List<Transaction> getTransactionsByUser(Connection connection, String accountname) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = """
                SELECT * FROM transactions 
                WHERE sender_accountname = ? OR receiver_accountname = ?
                ORDER BY created_at DESC
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountname);
            ps.setString(2, accountname);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(ResultSetMapper.mapToTransaction(rs));
                }
            }
        }
        return transactions;
    }

    /**
     * Retrieves a paged list of transactions involving a specific user.
     * @param connection The database connection.
     * @param accountname The account name.
     * @param limit The number of items to retrieve.
     * @param offset The number of items to skip.
     * @return A list of transactions.
     * @throws SQLException If a database error occurs.
     */
    public List<Transaction> getTransactionsPaged(Connection connection, String accountname, int limit, int offset) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = """
                SELECT * FROM transactions 
                WHERE sender_accountname = ? OR receiver_accountname = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountname);
            ps.setString(2, accountname);
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(ResultSetMapper.mapToTransaction(rs));
                }
            }
        }
        return transactions;
    }

    /**
     * Retrieves the total count of transactions for a specific user.
     * @param connection The database connection.
     * @param accountname The account name.
     * @return The total count.
     * @throws SQLException If a database error occurs.
     */
    public long getTotalTransactionsCount(Connection connection, String accountname) throws SQLException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE sender_accountname = ? OR receiver_accountname = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountname);
            ps.setString(2, accountname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }
}
