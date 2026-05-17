package org.example.server.service.product;

import org.example.model.product.Item;
import org.example.server.repository.ProductDao;
import org.example.util.FileLogger;

import java.sql.SQLException;
import java.util.List;

/**
 * Service for managing products and auction sessions.
 */
public class ProductService {
    private final ProductDao productDao;

    public ProductService(ProductDao productDao) {
        this.productDao = productDao;
    }

    public List<Item> getAllAuctions() {
        try {
            return productDao.getAllProducts();
        } catch (SQLException e) {
            FileLogger.error("Error fetching all products", e);
            return List.of();
        }
    }

    public Item getAuctionById(int productId) {
        try {
            return productDao.getProductById(productId);
        } catch (SQLException e) {
            FileLogger.error("Error fetching product by ID: " + productId, e);
            return null;
        }
    }

    public boolean createAuction(Item item) {
        try {
            return productDao.insertProduct(item);
        } catch (SQLException e) {
            FileLogger.error("Error creating auction for: " + item.getProductName(), e);
            return false;
        }
    }
}
