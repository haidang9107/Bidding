package org.example.server.service.product;

import org.example.model.product.Item;
import org.example.server.repository.DatabaseManager;
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

    public boolean createAuction(org.example.dto.ProductAddRequest addReq, String sellerAccount) {
        try (Connection conn = DatabaseManager.getConnection()) {
            Item item = switch (addReq.getCategory()) {
                case ELECTRONICS -> org.example.util.JsonConverter.fromJson(org.example.util.JsonConverter.toJson(addReq), org.example.model.product.Electronics.class);
                case ART -> org.example.util.JsonConverter.fromJson(org.example.util.JsonConverter.toJson(addReq), org.example.model.product.Art.class);
                case VEHICLE -> org.example.util.JsonConverter.fromJson(org.example.util.JsonConverter.toJson(addReq), org.example.model.product.Vehicle.class);
            };

            // Initialize mandatory auction fields
            item.setSellerAccountname(sellerAccount);
            item.setStatus(org.example.model.enums.AuctionStatus.RUNNING);
            item.setCurrentPrice(item.getStartingPrice());
            item.setStepPrice(item.getStartingPrice() / 10); // Default step price 10%

            if (item.getStartTime() == null) {
                item.setStartTime(new java.sql.Timestamp(System.currentTimeMillis()));
            }
            if (item.getEndTime() == null) {
                // Default end time: 7 days from now
                item.setEndTime(new java.sql.Timestamp(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L));
            }

            boolean success = productDao.insertProduct(conn, item);
            if (success) {
                // Real-time broadcast: Inform everyone about the new product
                org.example.payload.Response<Item> broadcastResponse = new org.example.payload.Response<>(
                    org.example.model.enums.MessageType.PRODUCT_LIST, true, "New product added!", item
                );
                org.example.server.network.Broadcaster.broadcast(broadcastResponse);
            }
            return success;
        } catch (SQLException e) {
            FileLogger.error("Error creating auction for: " + addReq.getName(), e);
            return false;
        }
    }
}
