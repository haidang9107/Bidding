package org.example.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * DAO for money movement and asset ownership audit records.
 */
public class TransactionDao {

    public boolean insertTransaction(Connection connection, String senderAccountname,
                                     String receiverAccountname, int type, Integer productId,
                                     long amount, Integer referenceId, String description)
            throws SQLException {
        String sql = """
                INSERT INTO transactions(
                    sender_accountname, receiver_accountname, type, product_id,
                    amount, reference_id, description
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
            ps.setInt(3, type);
            if (productId == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, productId);
            }
            ps.setLong(5, amount);
            if (referenceId == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, referenceId);
            }
            ps.setString(7, description);
            return ps.executeUpdate() > 0;
        }
    }
}
