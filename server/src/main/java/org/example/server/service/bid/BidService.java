package org.example.server.service.bid;

import org.example.model.Auction;
import org.example.model.product.Item;
import org.example.model.user.User;
import org.example.server.repository.AuctionDao;
import org.example.server.repository.ProductDao;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Service for handling bidding logic, including concurrency and balance checks.
 */
public class BidService {
    private final Connection connection;
    private final ProductDao productDao;
    private final UserDao userDao;
    private final AuctionDao auctionDao;

    public BidService(Connection connection) {
        this.connection = connection;
        this.productDao = new ProductDao(connection);
        this.userDao = new UserDao(connection);
        this.auctionDao = new AuctionDao(connection);
    }

    /**
     * Places a bid on a product.
     * Uses Pessimistic Locking and Ordered Balance Updates to prevent race conditions and deadlocks.
     */
    public String placeBid(int productId, int bidderId, long bidAmount) {
        try {
            connection.setAutoCommit(false);

            // 1. Lock Product first (Pessimistic Locking)
            Item item = productDao.getProductForUpdate(productId);
            User bidder = userDao.findById(bidderId);

            if (item == null || bidder == null) {
                return "Product or User not found.";
            }

            if (item.getSellerId() == bidderId) {
                return "Seller cannot bid on their own item.";
            }

            // 2. Basic Validation
            if (bidAmount < item.getCurrentPrice() + item.getStepPrice()) {
                return "Bid amount is too low. Minimum required: " + (item.getCurrentPrice() + item.getStepPrice());
            }

            long availableBalance = bidder.getBalance() - bidder.getBlockedBalance();
            if (bidAmount > availableBalance) {
                return "Insufficient balance. Available: " + availableBalance;
            }

            // 3. Handle Balance Updates in CONSISTENT ORDER to prevent deadlocks
            Integer oldWinnerId = item.getWinnerId();
            if (oldWinnerId != null) {
                if (oldWinnerId < bidderId) {
                    // Unlock old winner first
                    userDao.addBlockedBalance(oldWinnerId, -item.getCurrentPrice());
                    // Then lock new winner
                    userDao.addBlockedBalance(bidderId, bidAmount);
                } else if (oldWinnerId > bidderId) {
                    // Lock new winner first
                    userDao.addBlockedBalance(bidderId, bidAmount);
                    // Then unlock old winner
                    userDao.addBlockedBalance(oldWinnerId, -item.getCurrentPrice());
                } else {
                    // Same person bidding again
                    userDao.addBlockedBalance(bidderId, bidAmount - item.getCurrentPrice());
                }
            } else {
                // No old winner, just lock new winner
                userDao.addBlockedBalance(bidderId, bidAmount);
            }

            // 4. Update Product
            boolean success = productDao.updateBid(productId, bidAmount, bidderId, item.getVersion());
            if (!success) {
                connection.rollback();
                return "Concurrency error: Someone placed a bid faster than you. Please try again.";
            }

            // 5. Record Auction History
            Auction auction = new Auction(0, productId, bidderId, bidAmount, new Timestamp(System.currentTimeMillis()));
            auctionDao.insertAuction(auction);

            connection.commit();
            FileLogger.info("Bid placed successfully: User " + bidderId + " on Product " + productId);
            return "SUCCESS";

        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { /* Ignore */ }
            FileLogger.error("Bidding error", e);
            return "Internal Server Error: " + e.getMessage();
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { /* Ignore */ }
        }
    }
}
