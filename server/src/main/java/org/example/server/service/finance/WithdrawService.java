package org.example.server.service.finance;

import org.example.model.user.User;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service for handling withdrawal operations.
 */
import org.example.server.repository.DatabaseManager;
import org.example.model.user.User;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service for handling withdrawal operations.
 */
public class WithdrawService {
    private final UserDao userDao;

    public WithdrawService() {
        this.userDao = new UserDao();
    }

    public String withdraw(int userId, long amount) {
        if (amount <= 0) return "Invalid amount. Must be greater than 0.";

        try (Connection connection = DatabaseManager.getConnection()) {
            try {
                connection.setAutoCommit(false);
                
                User user = userDao.findByIdForUpdate(connection, userId);
                if (user == null) {
                    connection.rollback();
                    return "User not found.";
                }

                long availableBalance = user.getBalance() - user.getBlockedBalance();
                if (availableBalance < amount) {
                    connection.rollback();
                    return "Insufficient balance. Available: " + availableBalance;
                }

                userDao.addBalance(connection, userId, -amount);
                
                connection.commit();
                FileLogger.info("User " + userId + " withdrew " + amount);
                return "SUCCESS";
            } catch (SQLException e) {
                try { connection.rollback(); } catch (SQLException ex) { /* Ignore */ }
                throw e;
            }
        } catch (SQLException e) {
            FileLogger.error("Withdrawal error for user " + userId, e);
            return "Internal Error: " + e.getMessage();
        }
    }
}
