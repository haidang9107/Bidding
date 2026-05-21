package org.example.server.service.finance;

import org.example.model.user.User;
import org.example.server.repository.DatabaseManager;
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

    public String transfer(String fromAccount, String toAccount, long amount) {
        if (fromAccount.equals(toAccount)) return "Cannot transfer to yourself.";
        if (amount <= 0) return "Invalid amount. Must be greater than 0.";

        try (Connection connection = DatabaseManager.getConnection()) {
            try {
                connection.setAutoCommit(false);

                // 1. Lock accounts in a consistent order (alphabetical) to prevent deadlocks
                String firstAccount = fromAccount.compareTo(toAccount) < 0 ? fromAccount : toAccount;
                String secondAccount = fromAccount.compareTo(toAccount) < 0 ? toAccount : fromAccount;

                userDao.findByAccountnameForUpdate(connection, firstAccount);
                userDao.findByAccountnameForUpdate(connection, secondAccount);

                // 2. Refresh objects after locking
                User fromUser = userDao.findByAccountname(connection, fromAccount);
                User toUser = userDao.findByAccountname(connection, toAccount);

                if (fromUser == null || toUser == null) {
                    connection.rollback();
                    return "One or both users not found.";
                }

                if (!(fromUser instanceof org.example.model.user.Member fromMember) || 
                    !(toUser instanceof org.example.model.user.Member toMember)) {
                    connection.rollback();
                    return "Transfers are only available for members.";
                }

                // 3. Check balance
                long availableBalance = fromMember.getBalance() - fromMember.getBlockedBalance();
                if (availableBalance < amount) {
                    connection.rollback();
                    return "Insufficient balance. Available: " + availableBalance;
                }

                // 4. Execute transfer
                userDao.addBalance(connection, fromAccount, -amount);
                userDao.addBalance(connection, toAccount, amount);

                connection.commit();
                FileLogger.info("Transfer: " + amount + " from " + fromAccount + " to " + toAccount);
                return "SUCCESS";

            } catch (SQLException e) {
                try { connection.rollback(); } catch (SQLException ex) { /* Ignore */ }
                throw e;
            }
        } catch (SQLException e) {
            FileLogger.error("Transfer error from " + fromAccount + " to " + toAccount, e);
            return "Internal Error: " + e.getMessage();
        }
    }
}
