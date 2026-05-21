package org.example.server.service.finance;

import org.example.model.user.User;
import org.example.server.repository.DatabaseManager;
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

    public String withdraw(String accountname, long amount) {
        if (amount <= 0) return "Invalid amount. Must be greater than 0.";

        try (Connection connection = DatabaseManager.getConnection()) {
            try {
                connection.setAutoCommit(false);
                
                User user = userDao.findByAccountnameForUpdate(connection, accountname);
                if (user == null) {
                    connection.rollback();
                    return "User not found.";
                }

                if (!(user instanceof org.example.model.user.Member member)) {
                    connection.rollback();
                    return "Withdrawals are only available for members.";
                }

                long availableBalance = member.getBalance() - member.getBlockedBalance();
                if (availableBalance < amount) {
                    connection.rollback();
                    return "Insufficient balance. Available: " + availableBalance;
                }

                userDao.addBalance(connection, accountname, -amount);
                
                connection.commit();
                FileLogger.info("User " + accountname + " withdrew " + amount);
                return "SUCCESS";
            } catch (SQLException e) {
                try { connection.rollback(); } catch (SQLException ex) { /* Ignore */ }
                throw e;
            }
        } catch (SQLException e) {
            FileLogger.error("Withdrawal error for user " + accountname, e);
            return "Internal Error: " + e.getMessage();
        }
    }
}
