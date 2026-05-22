package org.example.server.repository;

import org.example.model.enums.AuctionStatus;
import org.example.model.enums.ItemCategory;
import org.example.model.product.Art;
import org.example.model.product.Electronics;
import org.example.model.product.Item;
import org.example.model.product.Vehicle;
import org.example.server.repository.mapper.ResultSetMapper;

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

    public Item getProductForUpdate(Connection connection, int auctionId) throws SQLException {
        return getAuctionForUpdate(connection, auctionId);
    }

    public boolean isUserLeadingAnyAuction(Connection connection, String accountname) throws SQLException {
        String sql = "SELECT COUNT(*) FROM auctions WHERE winner_accountname = ? AND status = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountname);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

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

    public boolean updateStatus(Connection connection, int auctionId, AuctionStatus status) throws SQLException {
        String sql = "UPDATE auctions SET status = ? WHERE auction_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, status.getValue());
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateProductAuctionFlag(Connection connection, int productId, boolean inAuction)
            throws SQLException {
        String sql = "UPDATE products SET is_in_auction = ? WHERE product_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, inAuction);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateProductOwner(Connection connection, int productId, String ownerAccountname)
            throws SQLException {
        String sql = "UPDATE products SET owner_accountname = ?, is_in_auction = FALSE WHERE product_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ownerAccountname);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        }
    }

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
            ps.setNull(6, Types.VARCHAR);
            ps.setNull(7, Types.INTEGER);
            ps.setNull(8, Types.VARCHAR);
            ps.setNull(9, Types.VARCHAR);
            ps.setNull(10, Types.VARCHAR);
            ps.setNull(11, Types.INTEGER);
        }
    }
}
