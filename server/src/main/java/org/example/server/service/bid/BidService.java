package org.example.server.service.bid;

import org.example.model.product.Item;
import org.example.model.user.User;
import org.example.model.user.Member;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.ProductDao;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service for handling bidding logic, including concurrency and balance checks.
 * Refactored to use accountname (String).
 */
public class BidService {
    private final ProductDao productDao;
    private final UserDao userDao;

    public BidService() {
        this.productDao = new ProductDao();
        this.userDao = new UserDao();
    }

    /**
     * Places a bid on a product.
     * Uses Pessimistic Locking and Ordered Balance Updates to prevent race conditions and deadlocks.
     */
    public String placeBid(int productId, String bidderAccountname, long bidAmount) {
        try (Connection connection = DatabaseManager.getConnection()) {
            try {
                connection.setAutoCommit(false);

                // 1. Lock Product
                Item item = productDao.getProductForUpdate(connection, productId);
                if (item == null) {
                    connection.rollback();
                    return "Product not found.";
                }

                // 2. Lock Users in CONSISTENT ORDER to prevent deadlocks
                String oldWinnerAccount = item.getWinnerAccountname();
                User bidder;
                
                if (oldWinnerAccount == null || oldWinnerAccount.equals(bidderAccountname)) {
                    // Only one user involved
                    bidder = userDao.findByAccountnameForUpdate(connection, bidderAccountname);
                } else {
                    // Two different users involved. Lock them in alphabetical order.
                    if (bidderAccountname.compareTo(oldWinnerAccount) < 0) {
                        bidder = userDao.findByAccountnameForUpdate(connection, bidderAccountname);
                        userDao.findByAccountnameForUpdate(connection, oldWinnerAccount);
                    } else {
                        userDao.findByAccountnameForUpdate(connection, oldWinnerAccount);
                        bidder = userDao.findByAccountnameForUpdate(connection, bidderAccountname);
                    }
                }

                if (bidder == null) {
                    connection.rollback();
                    return "User not found.";
                }

                if (!(bidder instanceof Member member)) {
                    connection.rollback();
                    return "Only members can place bids.";
                }

                if (item.getSellerAccountname().equals(bidderAccountname)) {
                    connection.rollback();
                    return "Seller cannot bid on their own item.";
                }

                // 3. Basic Validation
                if (bidAmount < item.getCurrentPrice() + item.getStepPrice()) {
                    connection.rollback();
                    return "Bid amount is too low. Minimum required: " + (item.getCurrentPrice() + item.getStepPrice());
                }

                long availableBalance = member.getBalance() - member.getBlockedBalance();
                if (bidAmount > availableBalance) {
                    connection.rollback();
                    return "Insufficient balance. Available: " + availableBalance;
                }

                // 4. Update Balances
                if (oldWinnerAccount != null) {
                    if (!oldWinnerAccount.equals(bidderAccountname)) {
                        // Different person: Release old winner's money, lock new bidder's money
                        userDao.addBlockedBalance(connection, oldWinnerAccount, -item.getCurrentPrice());
                        userDao.addBlockedBalance(connection, bidderAccountname, bidAmount);
                    } else {
                        // Same person bidding again: Adjust the difference
                        userDao.addBlockedBalance(connection, bidderAccountname, bidAmount - item.getCurrentPrice());
                    }
                } else {
                    // No old winner, just lock new winner
                    userDao.addBlockedBalance(connection, bidderAccountname, bidAmount);
                }

                // 5. Update Product using Optimistic Locking (with version check)
                boolean success = productDao.updateBid(connection, productId, bidAmount, bidderAccountname, item.getVersion());
                if (!success) {
                    connection.rollback();
                    return "Concurrency error: Someone placed a bid faster than you. Please try again.";
                }

                connection.commit();
                FileLogger.info("Bid placed successfully: User " + bidderAccountname + " on Product " + productId);
                return "SUCCESS";

            } catch (SQLException e) {
                try { connection.rollback(); } catch (SQLException ex) { /* Ignore */ }
                throw e;
            }
        } catch (SQLException e) {
            FileLogger.error("Bidding error", e);
            return "Internal Server Error: " + e.getMessage();
        }
    }
}
