package org.example.server.service.product;

import org.example.dto.response.PagedResponse;
import org.example.dto.response.ProductResponse;
import org.example.dto.request.ProductAddRequest;
import org.example.dto.notify.AuctionEndNotify;
import org.example.dto.notify.ProductUpdateNotify;
import org.example.model.enums.AuctionStatus;
import org.example.model.enums.MessageType;
import org.example.model.product.Item;
import org.example.payload.Response;
import org.example.server.exception.AuctionException;
import org.example.server.exception.NotFoundException;
import org.example.server.exception.ValidationException;
import org.example.server.network.Broadcaster;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.ProductDao;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Service for managing products and auction sessions.
 */
public class ProductService {
    private final ProductDao productDao;
    private final UserDao userDao;

    public ProductService() {
        this.productDao = new ProductDao();
        this.userDao = new UserDao();
    }

    /**
     * Periodically called by AuctionMonitor to close expired auctions.
     */
    public void processExpiredAuctions() {
        try (Connection conn = DatabaseManager.getConnection()) {
            List<Item> expiredItems = productDao.getExpiredProducts(conn);
            for (Item item : expiredItems) {
                finishAuction(item.getAuctionId());
            }
        } catch (SQLException e) {
            FileLogger.error("Error processing expired auctions", e);
        }
    }

    /**
     * Periodically called by AuctionMonitor to start scheduled auctions.
     */
    public void processUpcomingAuctions() {
        try (Connection conn = DatabaseManager.getConnection()) {
            List<Item> upcomingItems = productDao.getUpcomingProducts(conn);
            for (Item item : upcomingItems) {
                startAuction(item.getAuctionId());
            }
        } catch (SQLException e) {
            FileLogger.error("Error processing upcoming auctions", e);
        }
    }

    public void startAuction(int auctionId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            Item item = productDao.getAuctionForUpdate(conn, auctionId);
            if (item == null || item.getStatus() != AuctionStatus.OPEN) {
                conn.rollback();
                return;
            }

            boolean success = productDao.updateStatus(conn, auctionId, AuctionStatus.RUNNING);
            if (success) {
                conn.commit();
                FileLogger.info("Auction STARTED: Auction ID " + auctionId);
                Response<String> notification = new Response<>(
                    MessageType.AUCTION_START, true, "Auction '" + item.getName() + "' has started!", null
                );
                Broadcaster.broadcastToAuction(auctionId, notification);
            } else {
                conn.rollback();
            }
        } catch (SQLException e) {
            FileLogger.error("Error starting auction: " + auctionId, e);
        }
    }

    public void finishAuction(int auctionId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            
            Item latestItem = productDao.getAuctionForUpdate(conn, auctionId);
            if (latestItem == null || latestItem.getStatus() != AuctionStatus.RUNNING) {
                conn.rollback();
                return;
            }

            String winner = latestItem.getWinnerAccountname();
            String seller = latestItem.getSellerAccountname();
            long finalPrice = latestItem.getCurrentPrice();

            boolean success = productDao.updateStatus(conn, auctionId, AuctionStatus.FINISHED);
            
            if (success && winner != null) {
                // Deduct from winner's blocked balance
                userDao.addBlockedBalance(conn, winner, -finalPrice);
                // Add to seller's balance
                userDao.addBalance(conn, seller, finalPrice);
                
                productDao.updateProductOwner(conn, latestItem.getProductId(), winner);
            }

            if (!success) {
                conn.rollback();
                return;
            }

            conn.commit();
            
            if (winner != null) {
                FileLogger.info("Auction FINISHED: Auction ID " + auctionId + ", Winner: " + winner);
                ProductResponse productDetail = new ProductResponse(latestItem);
                Response<AuctionEndNotify> notification = new Response<>(
                    MessageType.AUCTION_END,
                    true,
                    "Auction for '" + latestItem.getName() + "' has ended!",
                    new AuctionEndNotify(
                        auctionId,
                        winner,
                        latestItem.getCurrentPrice(),
                        latestItem.getName(),
                        productDetail
                    )
                );
                Broadcaster.broadcastToAuction(auctionId, notification);
            } else {
                FileLogger.info("Auction FINISHED: Auction ID " + auctionId + " (No winner)");
            }
        } catch (SQLException e) {
            FileLogger.error("Error finalizing auction: " + auctionId, e);
        }
    }

    public List<Item> getAllAuctions() {
        try (Connection conn = DatabaseManager.getConnection()) {
            return productDao.getAllProducts(conn);
        } catch (SQLException e) {
            FileLogger.error("Error fetching all products", e);
            return List.of();
        }
    }

    public PagedResponse<Item> getAuctionsPaged(int page, int pageSize) {
        try (Connection conn = DatabaseManager.getConnection()) {
            long totalItems = productDao.getTotalProductsCount(conn);
            List<Item> items = productDao.getProductsPaged(conn, pageSize, (page - 1) * pageSize);
            return new PagedResponse<>(items, totalItems, page, pageSize);
        } catch (SQLException e) {
            FileLogger.error("Error fetching paged products", e);
            return new PagedResponse<>(List.of(), 0, page, pageSize);
        }
    }

    public Item getAuctionById(int id) {
        try (Connection conn = DatabaseManager.getConnection()) {
            Item item = productDao.getAuctionById(conn, id);
            return item != null ? item : productDao.getProductById(conn, id);
        } catch (SQLException e) {
            FileLogger.error("Error fetching auction/product by ID: " + id, e);
            throw new AuctionException("Database error fetching product");
        }
    }

    public void createAuction(ProductAddRequest addReq, String sellerAccount) {
        if (addReq == null) throw new ValidationException("Product data is required");

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            Item item = org.example.model.product.ItemFactory.createItem(
                addReq.getCategory(), 
                org.example.util.JsonConverter.toJson(addReq)
            );

            item.setSellerAccountname(sellerAccount);
            item.setStatus(AuctionStatus.RUNNING);
            item.setCurrentPrice(item.getStartingPrice());
            item.setStepPrice(Math.max(1, item.getStartingPrice() / 10));

            if (item.getStartTime() == null) {
                item.setStartTime(new Timestamp(System.currentTimeMillis()));
            }
            if (item.getEndTime() == null) {
                item.setEndTime(new Timestamp(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L));
            }

            boolean success = productDao.insertProduct(conn, item);
            if (success) {
                conn.commit();
                ProductResponse productResp = new ProductResponse(item);
                Response<ProductUpdateNotify> broadcastResponse = new Response<>(
                    MessageType.PRODUCT_LIST, true, "New product added!", 
                    new ProductUpdateNotify(productResp)
                );
                Broadcaster.broadcast(broadcastResponse);
            } else {
                conn.rollback();
                throw new AuctionException("Failed to insert product into database");
            }
        } catch (SQLException e) {
            FileLogger.error("Error creating auction for: " + addReq.getName(), e);
            throw new AuctionException("Database error during product creation");
        }
    }
}
