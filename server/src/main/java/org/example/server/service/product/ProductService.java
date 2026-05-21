package org.example.server.service.product;

import org.example.dto.PagedResponse;
import org.example.dto.ProductResponse;
import org.example.model.enums.AuctionStatus;
import org.example.model.enums.MessageType;
import org.example.model.product.Item;
import org.example.payload.Response;
import org.example.server.network.Broadcaster;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.ProductDao;
import org.example.server.repository.TransactionDao;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Service for managing products and auction sessions.
 */
public class ProductService {
    private final ProductDao productDao;
    private final UserDao userDao;
    private final TransactionDao transactionDao;

    public ProductService() {
        this.productDao = new ProductDao();
        this.userDao = new UserDao();
        this.transactionDao = new TransactionDao();
    }

    /**
     * Periodically called to close auctions that have reached their end time.
     */
    public void processExpiredAuctions() {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            List<Item> expired = productDao.getExpiredProducts(conn);
            for (Item item : expired) {
                // We re-fetch each auction with FOR UPDATE inside finishAuction
                finishAuction(conn, item.getAuctionId());
            }
            conn.commit();
        } catch (SQLException e) {
            FileLogger.error("Error processing expired auctions", e);
        }
    }

    private boolean finishAuction(Connection conn, int auctionId) throws SQLException {
        // Lock the row to prevent last-second bids while settling
        Item latestItem = productDao.getAuctionForUpdate(conn, auctionId);
        if (latestItem == null || latestItem.getStatus() != AuctionStatus.ACTIVE) {
            return false;
        }

        boolean success = productDao.updateStatus(conn, auctionId, AuctionStatus.FINISHED);
        productDao.updateProductAuctionFlag(conn, latestItem.getProductId(), false);

        String winner = latestItem.getWinnerAccountname();
        if (success && winner != null) {
            userDao.addBlockedBalance(conn, winner, -latestItem.getCurrentPrice());
            userDao.addBalance(conn, winner, -latestItem.getCurrentPrice());
            userDao.addBalance(conn, latestItem.getSellerAccountname(), latestItem.getCurrentPrice());
            productDao.updateProductOwner(conn, latestItem.getProductId(), winner);
            transactionDao.insertTransaction(conn, winner, latestItem.getSellerAccountname(), 3,
                    latestItem.getProductId(), latestItem.getCurrentPrice(), auctionId,
                    "Auction success for product " + latestItem.getProductId());
            
            FileLogger.info("Auction CLOSED: Auction ID " + auctionId + ", Winner: " + winner);
            
            // Broadcast notification
            Response<ProductResponse> notification = new Response<>(
                MessageType.AUCTION_END,
                true,
                "Auction for '" + latestItem.getName() + "' has ended!",
                new ProductResponse(latestItem)
            );
            Broadcaster.broadcastToAuction(auctionId, notification);
        } else if (success) {
            FileLogger.info("Auction CLOSED: Auction ID " + auctionId + " (No winner)");
        }
        return success;
    }

    public List<Item> getAllAuctions() {
        try (Connection conn = DatabaseManager.getConnection()) {
            return productDao.getAllProducts(conn);
        } catch (SQLException e) {
            FileLogger.error("Error fetching all products", e);
            return List.of();
        }
    }

    public org.example.dto.PagedResponse<Item> getAuctionsPaged(int page, int pageSize) {
        try (Connection conn = DatabaseManager.getConnection()) {
            long totalItems = productDao.getTotalProductsCount(conn);
            List<Item> items = productDao.getProductsPaged(conn, pageSize, (page - 1) * pageSize);
            return new org.example.dto.PagedResponse<>(items, totalItems, page, pageSize);
        } catch (SQLException e) {
            FileLogger.error("Error fetching paged products", e);
            return new org.example.dto.PagedResponse<>(List.of(), 0, page, pageSize);
        }
    }

    public Item getAuctionById(int id) {
        try (Connection conn = DatabaseManager.getConnection()) {
            Item item = productDao.getAuctionById(conn, id);
            return item != null ? item : productDao.getProductById(conn, id);
        } catch (SQLException e) {
            FileLogger.error("Error fetching auction/product by ID: " + id, e);
            return null;
        }
    }

    public boolean createAuction(org.example.dto.ProductAddRequest addReq, String sellerAccount) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            Item item = switch (addReq.getCategory()) {
                case ELECTRONICS -> org.example.util.JsonConverter.fromJson(org.example.util.JsonConverter.toJson(addReq), org.example.model.product.Electronics.class);
                case ART -> org.example.util.JsonConverter.fromJson(org.example.util.JsonConverter.toJson(addReq), org.example.model.product.Art.class);
                case VEHICLE -> org.example.util.JsonConverter.fromJson(org.example.util.JsonConverter.toJson(addReq), org.example.model.product.Vehicle.class);
            };

            // Initialize mandatory auction fields
            item.setSellerAccountname(sellerAccount);
            item.setOwnerAccountname(sellerAccount);
            item.setStatus(org.example.model.enums.AuctionStatus.ACTIVE);
            item.setCurrentPrice(item.getStartingPrice());
            item.setStepPrice(Math.max(1, item.getStartingPrice() / 10));

            if (item.getStartTime() == null) {
                item.setStartTime(new java.sql.Timestamp(System.currentTimeMillis()));
            }
            if (item.getEndTime() == null) {
                // Default end time: 7 days from now
                item.setEndTime(new java.sql.Timestamp(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L));
            }

            boolean success = productDao.insertProduct(conn, item);
            if (success) {
                conn.commit();
                // Real-time broadcast: Inform everyone about the new product
                org.example.payload.Response<Item> broadcastResponse = new org.example.payload.Response<>(
                    org.example.model.enums.MessageType.PRODUCT_LIST, true, "New product added!", item
                );
                org.example.server.network.Broadcaster.broadcast(broadcastResponse);
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException e) {
            FileLogger.error("Error creating auction for: " + addReq.getName(), e);
            return false;
        }
    }
}
