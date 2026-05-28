package org.example.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Data Access Object for logging auction room access.
 */
public class AuctionAccessDao {
    private static final AuctionAccessDao INSTANCE = new AuctionAccessDao();
    private AuctionAccessDao() {}
    public static AuctionAccessDao getInstance() { return INSTANCE; }

    /**
     * Logs a user's access to an auction room.
     * @param connection  The database connection.
     * @param accountname The user's account name.
     * @param auctionId   The auction ID.
     * @throws SQLException If a database error occurs.
     */
    public void logAccess(Connection connection, String accountname, int auctionId) throws SQLException {
        String sql = "INSERT INTO auction_access_logs (user_accountname, auction_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountname);
            ps.setInt(2, auctionId);
            ps.executeUpdate();
        }
    }
}
