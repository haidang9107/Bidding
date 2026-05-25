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
     * Retrieves all products owned by a given account. Useful for the
     * seller's "my inventory" screen.
     * @param connection       The database connection.
     * @param ownerAccountname The owner's account name.
     * @return A list of products (possibly empty).
     * @throws SQLException If a database error occurs.
     */
    public java.util.List<Product> getProductsByOwner(Connection connection,
                                                      String ownerAccountname) throws SQLException {
        String sql = PRODUCT_SELECT_SQL + "WHERE owner_accountname = ? ORDER BY product_id DESC";
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
            bindCategoryFields(ps, product);

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

    private void bindCategoryFields(PreparedStatement ps, Product product) throws SQLException {
        if (product instanceof Electronics e) {
            ps.setString(7, e.getBrand());
            ps.setInt(8, e.getWarrantyMonths());
            ps.setNull(9, Types.VARCHAR);
            ps.setNull(10, Types.VARCHAR);
            ps.setNull(11, Types.VARCHAR);
            ps.setNull(12, Types.INTEGER);
        } else if (product instanceof Art a) {
            ps.setNull(7, Types.VARCHAR);
            ps.setNull(8, Types.INTEGER);
            ps.setString(9, a.getArtist());
            ps.setString(10, a.getArtType());
            ps.setNull(11, Types.VARCHAR);
            ps.setNull(12, Types.INTEGER);
        } else if (product instanceof Vehicle v) {
            ps.setString(7, v.getBrand());
            ps.setNull(8, Types.INTEGER);
            ps.setNull(9, Types.VARCHAR);
            ps.setNull(10, Types.VARCHAR);
            ps.setString(11, v.getModel());
            ps.setInt(12, v.getManufactureYear());
        } else {
            // OtherItem or any other generic Product
            ps.setNull(7, Types.VARCHAR);
            ps.setNull(8, Types.INTEGER);
            ps.setNull(9, Types.VARCHAR);
            ps.setNull(10, Types.VARCHAR);
            ps.setNull(11, Types.VARCHAR);
            ps.setNull(12, Types.INTEGER);
        }
    }
}
