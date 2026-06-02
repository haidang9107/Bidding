package org.example.server.repository;

import org.example.model.product.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

/**
 * Data Access Object for the {@code products} table.
 * Holds only product (asset) concerns. Auction lifecycle lives in {@link AuctionDao}.
 */
public class ProductDao {

    private static final ProductDao INSTANCE = new ProductDao();
    private ProductDao() {}
    public static ProductDao getInstance() { return INSTANCE; }

    private static final String PRODUCT_SELECT_SQL =
            "SELECT product_id, name, description, image_url, category, owner_accountname, " +
            "       is_in_auction, withdrawn_at, brand, warranty_months, artist, art_type, " +
            "       model, manufacture_year " +
            "FROM products ";

    /**
     * Retrieves a product by its ID.
     * @param connection The database connection.
     * @param productId  The product ID.
     * @return The product, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    public Product getProductById(Connection connection, int productId) throws SQLException {
        String sql = PRODUCT_SELECT_SQL + "WHERE product_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ResultSetMapper.mapToProduct(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves a product by ID with a row lock (SELECT ... FOR UPDATE).
     * @param connection The database connection.
     * @param productId  The product ID.
     * @return The product, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    public Product getProductForUpdate(Connection connection, int productId) throws SQLException {
        String sql = PRODUCT_SELECT_SQL + "WHERE product_id = ? FOR UPDATE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ResultSetMapper.mapToProduct(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all products owned by a given account that have not been withdrawn.
     * Useful for the seller's "my inventory" screen.
     * @param connection       The database connection.
     * @param ownerAccountname The owner's account name.
     * @return A list of products (possibly empty).
     * @throws SQLException If a database error occurs.
     */
    public java.util.List<Product> getProductsByOwner(Connection connection,
                                                      String ownerAccountname) throws SQLException {
        String sql = PRODUCT_SELECT_SQL + "WHERE owner_accountname = ? AND withdrawn_at IS NULL ORDER BY product_id DESC";
        java.util.List<Product> out = new java.util.ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ownerAccountname);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(ResultSetMapper.mapToProduct(rs));
                }
            }
        }
        return out;
    }

    /**
     * Marks a product as withdrawn by setting the withdrawn_at timestamp.
     * @param connection The database connection.
     * @param productId  The product ID.
     * @return True if a row was updated.
     * @throws SQLException If a database error occurs.
     */
    public boolean withdrawProduct(Connection connection, int productId) throws SQLException {
        String sql = "UPDATE products SET withdrawn_at = CURRENT_TIMESTAMP WHERE product_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Inserts a new product and populates its generated product ID.
     * @param connection The database connection.
     * @param product    The product to insert.
     * @return True if successful.
     * @throws SQLException If a database error occurs.
     */
    public boolean insertProduct(Connection connection, Product product) throws SQLException {
        String sql = """
                INSERT INTO products(
                    name, description, image_url, category, owner_accountname, is_in_auction,
                    brand, warranty_months, artist, art_type, model, manufacture_year
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setString(3, product.getImageUrl());
            ps.setInt(4, product.getCategory().getValue());
            ps.setString(5, product.getOwnerAccountname());
            ps.setBoolean(6, product.isInAuction());
            bindCategoryFields(ps, product, 7);

            if (ps.executeUpdate() == 0) {
                return false;
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setProductId(generatedKeys.getInt(1));
                }
            }
        }
        return true;
    }

    /**
     * Updates an existing product's information.
     * @param connection The database connection.
     * @param product    The product with updated information.
     * @return True if successful.
     * @throws SQLException If a database error occurs.
     */
    public boolean updateProduct(Connection connection, Product product) throws SQLException {
        String sql = """
                UPDATE products SET
                    name = ?, description = ?, image_url = ?,
                    brand = ?, warranty_months = ?, artist = ?, art_type = ?,
                    model = ?, manufacture_year = ?
                WHERE product_id = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setString(3, product.getImageUrl());
            bindCategoryFields(ps, product, 4);
            ps.setInt(10, product.getProductId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates the {@code is_in_auction} flag of a product.
     * @param connection The database connection.
     * @param productId  The product ID.
     * @param inAuction  True if the product is currently in an auction.
     * @return True if a row was updated.
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
     * Transfers ownership of a product to a new account and clears the in-auction flag.
     * @param connection       The database connection.
     * @param productId        The product ID.
     * @param ownerAccountname The new owner.
     * @return True if a row was updated.
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
     * Gets the total number of products.
     * @param connection The database connection.
     * @return The total count.
     * @throws SQLException If a database error occurs.
     */
    public long getTotalProductsCount(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products")) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    /**
     * Gets the count of products by auction flag.
     * @param connection The database connection.
     * @param inAuction The in-auction flag.
     * @return The count.
     * @throws SQLException If a database error occurs.
     */
    public long countProductsByAuctionFlag(Connection connection, boolean inAuction) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE is_in_auction = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, inAuction);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return 0;
    }

    private void bindCategoryFields(PreparedStatement ps, Product product, int startIndex) throws SQLException {
        if (product instanceof Electronics e) {
            ps.setString(startIndex, e.getBrand());
            ps.setInt(startIndex + 1, e.getWarrantyMonths());
            ps.setNull(startIndex + 2, Types.VARCHAR);
            ps.setNull(startIndex + 3, Types.VARCHAR);
            ps.setNull(startIndex + 4, Types.VARCHAR);
            ps.setNull(startIndex + 5, Types.INTEGER);
        } else if (product instanceof Art a) {
            ps.setNull(startIndex, Types.VARCHAR);
            ps.setNull(startIndex + 1, Types.INTEGER);
            ps.setString(startIndex + 2, a.getArtist());
            ps.setString(startIndex + 3, a.getArtType());
            ps.setNull(startIndex + 4, Types.VARCHAR);
            ps.setNull(startIndex + 5, Types.INTEGER);
        } else if (product instanceof Vehicle v) {
            ps.setString(startIndex, v.getBrand());
            ps.setNull(startIndex + 1, Types.INTEGER);
            ps.setNull(startIndex + 2, Types.VARCHAR);
            ps.setNull(startIndex + 3, Types.VARCHAR);
            ps.setString(startIndex + 4, v.getModel());
            ps.setInt(startIndex + 5, v.getManufactureYear());
        } else {
            // OtherItem or any other generic Product
            ps.setNull(startIndex, Types.VARCHAR);
            ps.setNull(startIndex + 1, Types.INTEGER);
            ps.setNull(startIndex + 2, Types.VARCHAR);
            ps.setNull(startIndex + 3, Types.VARCHAR);
            ps.setNull(startIndex + 4, Types.VARCHAR);
            ps.setNull(startIndex + 5, Types.INTEGER);
        }
    }
}
