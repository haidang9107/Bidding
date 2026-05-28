package org.example.server.repository;

import org.example.model.Auction;
import org.example.model.product.Product;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the {@code watchlist} table.
 */
public class WatchlistDao {
    private static final WatchlistDao INSTANCE = new WatchlistDao();
    private WatchlistDao() {}
    public static WatchlistDao getInstance() { return INSTANCE; }

    /**
     * Adds a product to a user's watchlist.
     * @param connection  The database connection.
     * @param accountname The user's account name.
     * @param productId   The product ID.
     * @return True if successful.
     * @throws SQLException If a database error occurs.
     */
    public boolean addToWatchlist(Connection connection, String accountname, int productId) throws SQLException {
        String sql = "INSERT IGNORE INTO watchlist (user_accountname, product_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountname);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Removes a product from a user's watchlist.
     * @param connection  The database connection.
     * @param accountname The user's account name.
     * @param productId   The product ID.
     * @return True if successful.
     * @throws SQLException If a database error occurs.
     */
    public boolean removeFromWatchlist(Connection connection, String accountname, int productId) throws SQLException {
        String sql = "DELETE FROM watchlist WHERE user_accountname = ? AND product_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountname);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all products in a user's watchlist along with their current active auction (if any).
     * <p>
     * This method uses a LEFT JOIN to ensure products are returned even if they are not 
     * currently being auctioned. In such cases, the Auction fields (except for the 
     * embedded Product) will contain default/null values.
     * </p>
     * @param connection  The database connection.
     * @param accountname The user's account name.
     * @return A list of {@link Auction} objects acting as containers for the product and its status.
     * @throws SQLException If a database error occurs.
     */
    public List<Auction> getWatchedProductsWithLatestStatus(Connection connection, String accountname) throws SQLException {
        String sql = "SELECT p.product_id, p.name, p.description, p.image_url, p.category, p.owner_accountname, " +
                     "       p.is_in_auction, p.withdrawn_at, p.brand, p.warranty_months, p.artist, p.art_type, " +
                     "       p.model, p.manufacture_year, " +
                     "       a.auction_id, a.seller_accountname, a.winner_accountname, a.start_price, " +
                     "       a.step_price, a.current_price, a.buy_now_price, a.start_time, a.end_time, " +
                     "       a.status, a.version " +
                     "FROM watchlist w " +
                     "JOIN products p ON w.product_id = p.product_id " +
                     "LEFT JOIN auctions a ON p.product_id = a.product_id AND a.status IN (0, 1) " +
                     "WHERE w.user_accountname = ? " +
                     "ORDER BY w.created_at DESC";
        List<Auction> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountname);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Check if auction_id exists to determine which mapper to use
                    if (rs.getObject("auction_id") != null) {
                        out.add(ResultSetMapper.mapToAuctionWithProduct(rs));
                    } else {
                        // For products not in auction, create a skeleton Auction container
                        Auction container = new Auction();
                        container.setProduct(ResultSetMapper.mapToProduct(rs));
                        container.setProductId(container.getProduct().getProductId());
                        out.add(container);
                    }
                }
            }
        }
        return out;
    }

    /**
     * Retrieves all users watching a specific product.
     * @param connection  The database connection.
     * @param productId   The product ID.
     * @return A list of account names.
     * @throws SQLException If a database error occurs.
     */
    public List<String> getUsersWatchingProduct(Connection connection, int productId) throws SQLException {
        String sql = "SELECT user_accountname FROM watchlist WHERE product_id = ?";
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString("user_accountname"));
                }
            }
        }
        return out;
    }
}
