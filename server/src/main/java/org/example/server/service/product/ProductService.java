package org.example.server.service.product;

import org.example.dto.response.PagedResponse;
import org.example.dto.request.ProductAddRequest;
import org.example.model.enums.AuctionStatus;
import org.example.model.enums.TransactionType;
import org.example.model.product.Item;
import org.example.server.event.AuctionEndedEvent;
import org.example.server.event.AuctionStartedEvent;
import org.example.server.event.EventPublisher;
import org.example.server.event.ProductCreatedEvent;
import org.example.server.exception.AuctionException;
import org.example.server.exception.ValidationException;
import org.example.server.repository.ProductDao;
import org.example.server.repository.TransactionDao;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Timestamp;
import java.util.List;

/**
 * Service for managing products and auction sessions.
 * Refactored to use TransactionManager and EventPublisher.
 */
public class ProductService {
    private final ProductDao productDao;
    private final UserDao userDao;
    private final TransactionDao transactionDao;
    private final TransactionManager txManager;
    private final EventPublisher eventPublisher;
    private org.example.server.network.AuctionMonitor auctionMonitor;

    /**
     * Constructs a new ProductService.
     * @param txManager The transaction manager.
     * @param eventPublisher The event publisher.
     */
    public ProductService(TransactionManager txManager, EventPublisher eventPublisher) {
        this.productDao = new ProductDao();
        this.userDao = new UserDao();
        this.transactionDao = new TransactionDao();
        this.txManager = txManager;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Sets the auction monitor for this service.
     * @param auctionMonitor The auction monitor.
     */
    public void setAuctionMonitor(org.example.server.network.AuctionMonitor auctionMonitor) {
        this.auctionMonitor = auctionMonitor;
    }

    /**
     * Retrieves all currently running auctions.
     * @return A list of running items.
     */
    public List<Item> getAllRunningAuctions() {
        return txManager.query(productDao::getRunningProducts);
    }

    /**
     * Processes auctions that have expired and should be finished.
     */
    public void processExpiredAuctions() {
        List<Item> expired = txManager.query(productDao::getExpiredProducts);
        expired.forEach(item -> finishAuction(item.getAuctionId()));
    }

    /**
     * Processes auctions that are upcoming and should be started.
     */
    public void processUpcomingAuctions() {
        List<Item> upcoming = txManager.query(productDao::getUpcomingProducts);
        upcoming.forEach(item -> startAuction(item.getAuctionId()));
    }

    /**
     * Starts an auction.
     * @param auctionId The ID of the auction to start.
     */
    public void startAuction(int auctionId) {
        txManager.run(conn -> {
            Item item = productDao.getAuctionForUpdate(conn, auctionId);
            if (item == null || item.getStatus() != AuctionStatus.OPEN) return;

            boolean updated = productDao.updateStatus(conn, auctionId, AuctionStatus.RUNNING);
            if (updated) {
                FileLogger.info("Auction STARTED: Auction ID " + auctionId);
                
                // Lập lịch kết thúc chính xác
                if (auctionMonitor != null) {
                    auctionMonitor.scheduleAuctionEnd(auctionId, item.getEndTime());
                }

                eventPublisher.publish(new AuctionStartedEvent(auctionId, item.getName()));
            }
        });
    }

    /**
     * Alias for finishAuction to match AuctionMonitor terminology.
     * @param auctionId The ID of the auction.
     */
    public void processAuctionEnd(int auctionId) {
        finishAuction(auctionId);
    }

    /**
     * Finishes an auction and handles the winner and seller balances.
     * @param auctionId The ID of the auction to finish.
     */
    public void finishAuction(int auctionId) {
        txManager.run(conn -> {
            Item item = productDao.getAuctionForUpdate(conn, auctionId);
            if (item == null || item.getStatus() != AuctionStatus.RUNNING) return;

            String winner = item.getWinnerAccountname();
            String seller = item.getSellerAccountname();
            long finalPrice = item.getCurrentPrice();

            boolean success = productDao.updateStatus(conn, auctionId, AuctionStatus.FINISHED);
            if (success && winner != null) {
                userDao.addBalance(conn, winner, -finalPrice);
                userDao.addBlockedBalance(conn, winner, -finalPrice);
                userDao.addBalance(conn, seller, finalPrice);
                productDao.updateProductOwner(conn, item.getProductId(), winner);
                
                transactionDao.insertTransaction(conn, winner, seller, 
                        TransactionType.AUCTION_PAYMENT, item.getProductId(), finalPrice, 
                        auctionId, "Payment for auction win: " + item.getName());
            }

            if (success) {
                FileLogger.info("Auction FINISHED: Auction ID " + auctionId + (winner != null ? ", Winner: " + winner : " (No winner)"));
                eventPublisher.publish(new AuctionEndedEvent(auctionId, item.getName(), winner, finalPrice, false));
            }
        });
    }

    /**
     * Retrieves all auction items.
     * @return A list of all items.
     */
    public List<Item> getAllAuctions() {
        return txManager.query(productDao::getAllProducts);
    }

    /**
     * Retrieves a paged list of auctions.
     * @param page The page number (1-based).
     * @param pageSize The number of auctions per page.
     * @return A paged response containing auction items and metadata.
     */
    public PagedResponse<Item> getAuctionsPaged(int page, int pageSize) {
        return txManager.query(conn -> {
            long totalItems = productDao.getTotalProductsCount(conn);
            List<Item> items = productDao.getProductsPaged(conn, pageSize, (page - 1) * pageSize);
            return new PagedResponse<>(items, totalItems, page, pageSize);
        });
    }

    /**
     * Retrieves an auction by its ID.
     * @param id The auction ID.
     * @return The auction item, or null if not found.
     */
    public Item getAuctionById(int id) {
        return txManager.query(conn -> {
            Item item = productDao.getAuctionById(conn, id);
            return item != null ? item : productDao.getProductById(conn, id);
        });
    }

    /**
     * Creates a new auction.
     * @param addReq The product add request.
     * @param sellerAccount The account name of the seller.
     */
    public void createAuction(ProductAddRequest addReq, String sellerAccount) {
        if (addReq == null) throw new ValidationException("Product data is required");

        txManager.run(conn -> {
            Item item = org.example.model.product.ItemFactory.createItem(
                addReq.getCategory(), 
                org.example.util.JsonConverter.toJson(addReq)
            );

            item.setSellerAccountname(sellerAccount);
            item.setStatus(AuctionStatus.OPEN);
            item.setCurrentPrice(item.getStartingPrice());
            item.setStepPrice(Math.max(1, item.getStartingPrice() / 10));

            if (item.getStartTime() == null) {
                item.setStartTime(new Timestamp(System.currentTimeMillis()));
            }
            if (item.getEndTime() == null) {
                item.setEndTime(new Timestamp(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L));
            }

            if (productDao.insertProduct(conn, item)) {
                // Lập lịch bắt đầu hoặc kết thúc chính xác cho phiên vừa tạo
                if (auctionMonitor != null) {
                    if (item.getStatus() == AuctionStatus.OPEN) {
                        auctionMonitor.scheduleAuctionStart(item.getAuctionId(), item.getStartTime());
                    } else if (item.getStatus() == AuctionStatus.RUNNING) {
                        auctionMonitor.scheduleAuctionEnd(item.getAuctionId(), item.getEndTime());
                    }
                }
                eventPublisher.publish(new ProductCreatedEvent(item.getAuctionId()));
            } else {
                throw new AuctionException("Failed to insert product into database");
            }
        });
    }
}