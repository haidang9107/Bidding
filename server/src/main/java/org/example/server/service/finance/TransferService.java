package org.example.server.service.finance;

import org.example.model.user.User;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service for handling money transfers between users.
 * Implements deadlock prevention by locking accounts in a consistent order.
 */
import org.example.server.repository.DatabaseManager;
import org.example.model.user.User;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service for handling money transfers between users.
 * Implements deadlock prevention by locking accounts in a consistent order.
 */
public class TransferService {
    private final UserDao userDao;

    public TransferService() {
        this.userDao = new UserDao();
    }

    public String transfer(int fromUserId, int toUserId, long amount) {
        if (fromUserId == toUserId) return "Cannot transfer to yourself.";
        if (amount <= 0) return "Invalid amount. Must be greater than 0.";

        try (Connection connection = DatabaseManager.getConnection()) {
            try {
                connection.setAutoCommit(false);

                // 1. Lock accounts in a consistent order (by ID) to prevent deadlocks
                int firstId = Math.min(fromUserId, toUserId);
                int secondId = Math.max(fromUserId, toUserId);

                userDao.findByIdForUpdate(connection, firstId);
                userDao.findByIdForUpdate(connection, secondId);

                // 2. Refresh objects after locking
                User fromUser = userDao.findById(connection, fromUserId);
                User toUser = userDao.findById(connection, toUserId);

                if (fromUser == null || toUser == null) {
                    connection.rollback();
                    return "One or both users not found.";
                }

                // 3. Check balance
                long availableBalance = fromUser.getBalance() - fromUser.getBlockedBalance();
                if (availableBalance < amount) {
                    connection.rollback();
                    return "Insufficient balance. Available: " + availableBalance;
                }

                // 4. Execute transfer
                userDao.addBalance(connection, fromUserId, -amount);
                userDao.addBalance(connection, toUserId, amount);

                connection.commit();
                FileLogger.info("Transfer: " + amount + " from " + fromUserId + " to " + toUserId);
                return "SUCCESS";

            } catch (SQLException e) {
                try { connection.rollback(); } catch (SQLException ex) { /* Ignore */ }
                throw e;
            }
        } catch (SQLException e) {
            FileLogger.error("Transfer error from " + fromUserId + " to " + toUserId, e);
            return "Internal Error: " + e.getMessage();
        }
    }
}
