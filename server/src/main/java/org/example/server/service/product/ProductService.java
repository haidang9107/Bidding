package org.example.server.service.product;

import org.example.model.product.Item;
import org.example.server.repository.ProductDao;
import org.example.util.FileLogger;

import java.sql.SQLException;
import java.util.List;

/**
 * Service for managing products and auction sessions.
 */
import org.example.server.repository.DatabaseManager;
import org.example.model.product.Item;
import org.example.server.repository.ProductDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Service for managing products and auction sessions.
 */
public class ProductService {
    private final ProductDao productDao;

    public ProductService() {
        this.productDao = new ProductDao();
    }

    public List<Item> getAllAuctions() {
        try (Connection conn = DatabaseManager.getConnection()) {
            return productDao.getAllProducts(conn);
        } catch (SQLException e) {
            FileLogger.error("Error fetching all products", e);
            return List.of();
        }
    }

    public Item getAuctionById(int productId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return productDao.getProductById(conn, productId);
        } catch (SQLException e) {
            FileLogger.error("Error fetching product by ID: " + productId, e);
            return null;
        }
    }

    public boolean createAuction(Item item) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return productDao.insertProduct(conn, item);
        } catch (SQLException e) {
            FileLogger.error("Error creating auction for: " + item.getProductName(), e);
            return false;
        }
    }
}
