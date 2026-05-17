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
     * Implements transaction management and optimistic locking.
     */
    public synchronized String placeBid(int productId, int bidderId, long bidAmount) {
        try {
            // 1. Transaction Start (Note: We use the shared connection)
            connection.setAutoCommit(false);

            // 2. Load Item and Bidder
            Item item = productDao.getProductById(productId);
            User bidder = userDao.findById(bidderId);

            if (item == null || bidder == null) {
                return "Product or User not found.";
            }

            // 3. Basic Validation
            if (bidAmount < item.getCurrentPrice() + item.getStepPrice()) {
                return "Bid amount is too low. Minimum required: " + (item.getCurrentPrice() + item.getStepPrice());
            }

            long availableBalance = bidder.getBalance() - bidder.getBlockedBalance();
            if (bidAmount > availableBalance) {
                return "Insufficient balance. Available: " + availableBalance;
            }

            // 4. Handle Old Winner (Unlock their balance)
            if (item.getWinnerId() != null) {
                User oldWinner = userDao.findById(item.getWinnerId());
                if (oldWinner != null) {
                    userDao.updateBalance(oldWinner.getUserId(), oldWinner.getBalance(), 
                            oldWinner.getBlockedBalance() - item.getCurrentPrice());
                }
            }

            // 5. Update New Winner (Lock their balance)
            userDao.updateBalance(bidderId, bidder.getBalance(), bidder.getBlockedBalance() + bidAmount);

            // 6. Update Product with Optimistic Locking
            boolean success = productDao.updateBid(productId, bidAmount, bidderId, item.getVersion());
            if (!success) {
                connection.rollback();
                return "Concurrency error: Someone placed a bid faster than you. Please try again.";
            }

            // 7. Record Auction History
            Auction auction = new Auction(0, productId, bidderId, bidAmount, new Timestamp(System.currentTimeMillis()));
            auctionDao.insertAuction(auction);

            // 8. Commit
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
