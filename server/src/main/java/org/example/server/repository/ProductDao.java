package org.example.server.repository;

import org.example.model.enums.AuctionStatus;
import org.example.model.product.*;

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
 * Data Access Object for product assets and auction sessions.
 */
public class ProductDao {

    private static final String AUCTION_VIEW_SQL = """
            SELECT p.*, a.auction_id, a.seller_accountname, a.winner_accountname,
                   a.start_price, a.step_price, a.current_price, a.buy_now_price,
                   a.start_time, a.end_time, a.status, a.version
            FROM auctions a
            JOIN products p ON p.product_id = a.product_id
            """;

    /**
     * Retrieves a page of auction items from the database.
     * @param connection The database connection.
     * @param limit The maximum number of items to retrieve.
     * @param offset The number of items to skip.
     * @return A list of items.
     * @throws SQLException If a database error occurs.
     */
    public List<Item> getProductsPaged(Connection connection, int limit, int offset) throws SQLException {
        List<Item> products = new ArrayList<>();
        String sql = AUCTION_VIEW_SQL + " ORDER BY a.created_at DESC LIMIT ? OFFSET ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(ResultSetMapper.mapToItem(rs));
                }
            }
        }
        return products;
    }

    /**
     * Retrieves the total count of auction items in the database.
     * @param connection The database connection.
     * @return The total count.
     * @throws SQLException If a database error occurs.
     */
    public long getTotalProductsCount(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM auctions";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    /**
     * Retrieves all auction items from the database.
     * @param connection The database connection.
     * @return A list of all items.
     * @throws SQLException If a database error occurs.
     */
    public List<Item> getAllProducts(Connection connection) throws SQLException {
        List<Item> products = new ArrayList<>();
        String sql = AUCTION_VIEW_SQL + " ORDER BY a.created_at DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(ResultSetMapper.mapToItem(rs));
            }
        }
        return products;
    }

    /**
     * Retrieves a specific product by its ID.
     * @param connection The database connection.
     * @param productId The ID of the product.
     * @return The product item, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    public Item getProductById(Connection connection, int productId) throws SQLException {
        String sql = AUCTION_VIEW_SQL + " WHERE p.product_id = ? ORDER BY a.created_at DESC LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ResultSetMapper.mapToItem(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves a specific auction by its ID.
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @return The auction item, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    public Item getAuctionById(Connection connection, int auctionId) throws SQLException {
        String sql = AUCTION_VIEW_SQL + " WHERE a.auction_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ResultSetMapper.mapToItem(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves a specific auction and locks the row for update.
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @return The auction item, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    public Item getAuctionForUpdate(Connection connection, int auctionId) throws SQLException {
        String sql = AUCTION_VIEW_SQL + " WHERE a.auction_id = ? FOR UPDATE";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ResultSetMapper.mapToItem(rs);
                }
            }
        }
        return null;
    }

    /**
     * Convenience method to get an auction for update.
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @return The auction item.
     * @throws SQLException If a database error occurs.
     */
    public Item getProductForUpdate(Connection connection, int auctionId) throws SQLException {
        return getAuctionForUpdate(connection, auctionId);
    }

    /**
     * Checks if a user is currently the leading bidder in any running auction.
     * @param connection The database connection.
     * @param accountname The account name of the user.
     * @return True if leading, false otherwise.
     * @throws SQLException If a database error occurs.
     */
    public boolean isUserLeadingAnyAuction(Connection connection, String accountname) throws SQLException {
        String sql = "SELECT COUNT(*) FROM auctions WHERE winner_accountname = ? AND status = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountname);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Inserts a new product and its associated auction into the database.
     * @param connection The database connection.
     * @param item The item to insert.
     * @return True if successful.
     * @throws SQLException If a database error occurs.
     */
    public boolean insertProduct(Connection connection, Item item) throws SQLException {
        String productSql = """
                INSERT INTO products(
                    name, description, image_url, category, owner_accountname, is_in_auction,
                    brand, warranty_months, artist, art_type, model, manufacture_year
                ) VALUES (?, ?, ?, ?, ?, TRUE, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(productSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setString(3, item.getImageUrl());
            ps.setInt(4, item.getCategory().getValue());
            ps.setString(5, item.getSellerAccountname());
            bindCategoryFields(ps, item);

            if (ps.executeUpdate() == 0) {
                return false;
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setProductId(generatedKeys.getInt(1));
                }
            }
        }

        String auctionSql = """
                INSERT INTO auctions(
                    product_id, seller_accountname, start_price, step_price, current_price,
                    buy_now_price, start_time, end_time, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(auctionSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getProductId());
            ps.setString(2, item.getSellerAccountname());
            ps.setLong(3, item.getStartingPrice());
            ps.setLong(4, item.getStepPrice());
            ps.setLong(5, item.getCurrentPrice());
            if (item.getBuyNowPrice() == null) {
                ps.setNull(6, Types.BIGINT);
            } else {
                ps.setLong(6, item.getBuyNowPrice());
            }
            ps.setTimestamp(7, item.getStartTime());
            ps.setTimestamp(8, item.getEndTime());
            ps.setInt(9, item.getStatus().getValue());

            if (ps.executeUpdate() == 0) {
                return false;
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setAuctionId(generatedKeys.getInt(1));
                }
            }
        }
        return true;
    }

    /**
     * Retrieves all running auctions that have expired.
     * @param connection The database connection.
     * @return A list of expired items.
     * @throws SQLException If a database error occurs.
     */
    public List<Item> getExpiredProducts(Connection connection) throws SQLException {
        List<Item> products = new ArrayList<>();
        String sql = AUCTION_VIEW_SQL + " WHERE a.status = 1 AND a.end_time <= CURRENT_TIMESTAMP";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(ResultSetMapper.mapToItem(rs));
            }
        }
        return products;
    }

    /**
     * Retrieves all upcoming auctions that are ready to start.
     * @param connection The database connection.
     * @return A list of upcoming items.
     * @throws SQLException If a database error occurs.
     */
    public List<Item> getUpcomingProducts(Connection connection) throws SQLException {
        List<Item> products = new ArrayList<>();
        String sql = AUCTION_VIEW_SQL + " WHERE a.status = 0 AND a.start_time <= CURRENT_TIMESTAMP";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(ResultSetMapper.mapToItem(rs));
            }
        }
        return products;
    }

    /**
     * Updates the status of an auction.
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @param status The new status.
     * @return True if successful.
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
     * Updates the 'in auction' flag for a product.
     * @param connection The database connection.
     * @param productId The ID of the product.
     * @param inAuction Whether the product is in an auction.
     * @return True if successful.
     * @throws SQLException If a database error occurs.
     */
    public boolean updateProductAuctionFlag(Connection connection, int productId, boolean inAuction)
            throws SQLException {
        String sql = "UPDATE products SET is_in_auction = ? WHERE product_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, inAuction);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates the owner of a product and marks it as no longer in auction.
     * @param connection The database connection.
     * @param productId The ID of the product.
     * @param ownerAccountname The new owner's account name.
     * @return True if successful.
     * @throws SQLException If a database error occurs.
     */
    public boolean updateProductOwner(Connection connection, int productId, String ownerAccountname)
            throws SQLException {
        String sql = "UPDATE products SET owner_accountname = ?, is_in_auction = FALSE WHERE product_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ownerAccountname);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates the current bid and winner for an auction using optimistic locking.
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @param newPrice The new bid amount.
     * @param bidderAccountname The new winner's account name.
     * @param oldVersion The current version for optimistic locking.
     * @return True if successful.
     * @throws SQLException If a database error occurs.
     */
    public boolean updateBid(Connection connection, int auctionId, long newPrice,
                             String bidderAccountname, int oldVersion) throws SQLException {
        String sql = """
                UPDATE auctions
                SET current_price = ?, winner_accountname = ?, version = version + 1
                WHERE auction_id = ? AND version = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, newPrice);
            ps.setString(2, bidderAccountname);
            ps.setInt(3, auctionId);
            ps.setInt(4, oldVersion);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates the current bid and winner for an auction without optimistic locking (assumes row lock).
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @param newPrice The new bid amount.
     * @param bidderAccountname The new winner's account name.
     * @return True if successful.
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
     * Updates the end time of an auction.
     * @param connection The database connection.
     * @param auctionId The ID of the auction.
     * @param newEndTime The new end time.
     * @return True if successful.
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

    private void bindCategoryFields(PreparedStatement ps, Item item) throws SQLException {
        if (item instanceof Electronics e) {
            ps.setString(6, e.getBrand());
            ps.setInt(7, e.getWarrantyMonths());
            ps.setNull(8, Types.VARCHAR);
            ps.setNull(9, Types.VARCHAR);
            ps.setNull(10, Types.VARCHAR);
            ps.setNull(11, Types.INTEGER);
        } else if (item instanceof Art a) {
            ps.setNull(6, Types.VARCHAR);
            ps.setNull(7, Types.INTEGER);
            ps.setString(8, a.getArtist());
            ps.setString(9, a.getArtType());
            ps.setNull(10, Types.VARCHAR);
            ps.setNull(11, Types.INTEGER);
        } else if (item instanceof Vehicle v) {
            ps.setString(6, v.getBrand());
            ps.setNull(7, Types.INTEGER);
            ps.setNull(8, Types.VARCHAR);
            ps.setNull(9, Types.VARCHAR);
            ps.setString(10, v.getModel());
            ps.setInt(11, v.getManufactureYear());
        } else {
            // OtherItem or any other generic Item
            ps.setNull(6, Types.VARCHAR);
            ps.setNull(7, Types.INTEGER);
            ps.setNull(8, Types.VARCHAR);
            ps.setNull(9, Types.VARCHAR);
            ps.setNull(10, Types.VARCHAR);
            ps.setNull(11, Types.INTEGER);
        }
    }
}