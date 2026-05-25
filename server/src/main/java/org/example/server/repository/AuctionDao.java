package org.example.server.repository;

import org.example.dto.response.PagedResponse;
import org.example.model.Auction;
import org.example.model.enums.AuctionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the {@code auctions} table.
 * For queries that also need product info (list/detail views), the SQL joins
 * the {@code products} table and {@link ResultSetMapper} eagerly loads the product
 * into the returned {@link Auction}.
 */
public class AuctionDao {

    private static final String AUCTION_SELECT_SQL =
            "SELECT a.auction_id, a.product_id, a.seller_accountname, a.winner_accountname, " +
            "       a.start_price, a.step_price, a.current_price, a.buy_now_price, " +
            "       a.start_time, a.end_time, a.status, a.version " +
            "FROM auctions a ";

    private static final String AUCTION_JOIN_PRODUCT_SQL =
            "SELECT a.auction_id, a.product_id, a.seller_accountname, a.winner_accountname, " +
            "       a.start_price, a.step_price, a.current_price, a.buy_now_price, " +
            "       a.start_time, a.end_time, a.status, a.version, " +
            "       p.name, p.description, p.image_url, p.category, p.owner_accountname, " +
            "       p.is_in_auction, p.withdrawn_at, " +
            "       p.brand, p.warranty_months, p.artist, p.art_type, p.model, p.manufacture_year " +
            "FROM auctions a JOIN products p ON p.product_id = a.product_id ";

    /**
     * Retrieves a single auction by ID, with its product eagerly loaded.
     * @param connection The database connection.
     * @param auctionId  The auction ID.
     * @return The auction with its product, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    public Auction getAuctionById(Connection connection, int auctionId) throws SQLException {
        String sql = AUCTION_JOIN_PRODUCT_SQL + "WHERE a.auction_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ResultSetMapper.mapToAuctionWithProduct(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves an auction by ID with a row lock (SELECT ... FOR UPDATE),
     * and eagerly loads its product.
     * @param connection The database connection.
     * @param auctionId  The auction ID.
     * @return The auction, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    public Auction getAuctionForUpdate(Connection connection, int auctionId) throws SQLException {
        String sql = AUCTION_JOIN_PRODUCT_SQL + "WHERE a.auction_id = ? FOR UPDATE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ResultSetMapper.mapToAuctionWithProduct(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves a paged list of auctions, with each auction's product eagerly loaded.
     * @param connection The database connection.
     * @param limit      The maximum number of rows.
     * @param offset     The number of rows to skip.
     * @return A list of auctions.
     * @throws SQLException If a database error occurs.
     */
    public List<Auction> getAuctionsPaged(Connection connection, int limit, int offset) throws SQLException {
        List<Auction> auctions = new ArrayList<>();
        String sql = AUCTION_JOIN_PRODUCT_SQL + "ORDER BY a.created_at DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    auctions.add(ResultSetMapper.mapToAuctionWithProduct(rs));
                }
            }
        }
        return auctions;
    }

    /**
     * Returns the total number of auctions.
     * @param connection The database connection.
     * @return The total count.
     * @throws SQLException If a database error occurs.
     */
    public long getTotalAuctionsCount(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM auctions")) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    /**
     * Convenience helper for paged auction queries.
     * @param connection The database connection.
     * @param page       The 1-based page number.
     * @param pageSize   The page size.
     * @return A paged response of auctions (products loaded).
     * @throws SQLException If a database error occurs.
     */
    public PagedResponse<Auction> getAuctionsPagedResponse(Connection connection, int page, int pageSize)
            throws SQLException {
        long total = getTotalAuctionsCount(connection);
        List<Auction> items = getAuctionsPaged(connection, pageSize, (page - 1) * pageSize);
        return new PagedResponse<>(items, total, page, pageSize);
    }

    /**
     * Retrieves all currently running auctions, with products loaded.
     * @param connection The database connection.
     * @return A list of running auctions.
     * @throws SQLException If a database error occurs.
     */
    public List<Auction> getRunningAuctions(Connection connection) throws SQLException {
        List<Auction> auctions = new ArrayList<>();
        String sql = AUCTION_JOIN_PRODUCT_SQL + "WHERE a.status = ? ORDER BY a.created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, AuctionStatus.RUNNING.getValue());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    auctions.add(ResultSetMapper.mapToAuctionWithProduct(rs));
                }
            }
        }
        return auctions;
    }

    /**
     * Retrieves all RUNNING auctions whose end time has passed.
     * @param connection The database connection.
     * @return A list of expired auctions.
     * @throws SQLException If a database error occurs.
     */
    public List<Auction> getExpiredAuctions(Connection connection) throws SQLException {
        List<Auction> auctions = new ArrayList<>();
        String sql = AUCTION_JOIN_PRODUCT_SQL +
                "WHERE a.status = ? AND a.end_time <= CURRENT_TIMESTAMP";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, AuctionStatus.RUNNING.getValue());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    auctions.add(ResultSetMapper.mapToAuctionWithProduct(rs));
                }
            }
        }
        return auctions;
    }

    /**
     * Retrieves all OPEN auctions whose start time has passed (ready to start).
     * @param connection The database connection.
     * @return A list of upcoming auctions.
     * @throws SQLException If a database error occurs.
     */
    public List<Auction> getUpcomingAuctions(Connection connection) throws SQLException {
        List<Auction> auctions = new ArrayList<>();
        String sql = AUCTION_JOIN_PRODUCT_SQL +
                "WHERE a.status = ? AND a.start_time <= CURRENT_TIMESTAMP";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, AuctionStatus.OPEN.getValue());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    auctions.add(ResultSetMapper.mapToAuctionWithProduct(rs));
                }
            }
        }
        return auctions;
    }

    /**
     * Checks if a user is currently leading any RUNNING auction.
     * @param connection  The database connection.
     * @param accountname The user's account name.
     * @return True if the user is leading at least one running auction.
     * @throws SQLException If a database error occurs.
     */
    public boolean isUserLeadingAnyAuction(Connection connection, String accountname) throws SQLException {
        String sql = "SELECT COUNT(*) FROM auctions WHERE winner_accountname = ? AND status = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountname);
            ps.setInt(2, AuctionStatus.RUNNING.getValue());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Inserts a new auction row and populates the generated auction ID.
     * @param connection The database connection.
     * @param auction    The auction to insert.
     * @return True if successful.
     * @throws SQLException If a database error occurs.
     */
    public boolean insertAuction(Connection connection, Auction auction) throws SQLException {
        String sql = """
                INSERT INTO auctions(
                    product_id, seller_accountname, start_price, step_price, current_price,
                    buy_now_price, start_time, end_time, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, auction.getProductId());
            ps.setString(2, auction.getSellerAccountname());
            ps.setLong(3, auction.getStartingPrice());
            ps.setLong(4, auction.getStepPrice());
            ps.setLong(5, auction.getCurrentPrice());
            if (auction.getBuyNowPrice() == null) {
                ps.setNull(6, Types.BIGINT);
            } else {
                ps.setLong(6, auction.getBuyNowPrice());
            }
            ps.setTimestamp(7, auction.getStartTime());
            ps.setTimestamp(8, auction.getEndTime());
            ps.setInt(9, auction.getStatus().getValue());

            if (ps.executeUpdate() == 0) return false;
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    auction.setAuctionId(generatedKeys.getInt(1));
                }
            }
        }
        return true;
    }

    /**
     * Updates the status of an auction.
     * @param connection The database connection.
     * @param auctionId  The auction ID.
     * @param status     The new status.
     * @return True if a row was updated.
     * @throws SQLException If a database error occurs.
     */
    public boolean updateStatus(Connection connection, int auctionId, AuctionStatus status) throws SQLException {
        String sql = "UPDATE auctions SET status = ? WHERE auction_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, status.getValue());
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates the current bid (price + winner) of an auction. Assumes the row is already locked.
     * @param connection         The database connection.
     * @param auctionId          The auction ID.
     * @param newPrice           The new current price.
     * @param bidderAccountname  The new winner.
     * @return True if a row was updated.
     * @throws SQLException If a database error occurs.
     */
    public boolean updateBidLocked(Connection connection, int auctionId, long newPrice,
                                   String bidderAccountname) throws SQLException {
        String sql = """
                UPDATE auctions
                SET current_price = ?, winner_accountname = ?, version = version + 1
                WHERE auction_id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, newPrice);
            ps.setString(2, bidderAccountname);
            ps.setInt(3, auctionId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates the end time of an auction (used by anti-snipping).
     * @param connection The database connection.
     * @param auctionId  The auction ID.
     * @param newEndTime The new end time.
     * @return True if a row was updated.
     * @throws SQLException If a database error occurs.
     */
    public boolean updateAuctionEndTime(Connection connection, int auctionId, Timestamp newEndTime)
            throws SQLException {
        String sql = "UPDATE auctions SET end_time = ? WHERE auction_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, newEndTime);
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
        }
    }
}
