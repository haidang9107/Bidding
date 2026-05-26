package org.example.server.service.product;

import org.example.model.product.Product;
import org.example.server.repository.ProductDao;
import org.example.server.repository.TransactionManager;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service handling pure {@code Product} concerns: lookup and ownership transfer.
 * Auction lifecycle (create / start / finish / cancel) is handled by
 * {@code AuctionService}.
 */
public class ProductService {
    private final ProductDao productDao;
    private final TransactionManager txManager;

    /**
     * Constructs a ProductService.
     * @param txManager The transaction manager.
     */
    public ProductService(TransactionManager txManager) {
        this.productDao = ProductDao.getInstance();
        this.txManager = txManager;
    }

    /**
     * Retrieves a product by ID.
     * @param productId The product ID.
     * @return The product, or null if not found.
     */
    public Product getProductById(int productId) {
        return txManager.query(conn -> productDao.getProductById(conn, productId));
    }

    /**
     * Lists all products owned by a given account, used for the
     * seller's "kho" (inventory) view.
     * @param ownerAccountname The owner's account name.
     * @return A list of products (may be empty).
     */
    public java.util.List<Product> getProductsByOwner(String ownerAccountname) {
        return txManager.query(conn -> productDao.getProductsByOwner(conn, ownerAccountname));
    }

    /**
     * Inserts a new product into the seller's inventory WITHOUT opening an
     * auction. The product is created with {@code isInAuction = false}.
     * @param product The product to insert. Owner must already be set.
     * @return The generated product ID.
     */
    public int createInventoryProduct(Product product) {
        return txManager.execute(conn -> {
            product.setInAuction(false);
            if (!productDao.insertProduct(conn, product)) {
                throw new org.example.server.exception.DatabaseException(
                        "Failed to insert product into database");
            }
            return product.getProductId();
        });
    }

    /**
     * Transfers the ownership of a product to a new account and clears the
     * in-auction flag. Designed to be called from within an existing transaction
     * (typically when an auction is being finished).
     *
     * @param connection       An open, transactional connection.
     * @param productId        The product ID.
     * @param newOwnerAccount  The new owner's account name.
     * @return True if a row was updated.
     * @throws SQLException If a database error occurs.
     */
    public boolean transferOwnership(Connection connection, int productId, String newOwnerAccount)
            throws SQLException {
        return productDao.updateProductOwner(connection, productId, newOwnerAccount);
    }
}
