package org.example.server.service.finance;

import org.example.model.user.User;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.TransactionDao;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service for handling deposit operations.
 */
public class DepositService {
    private final UserDao userDao;
    private final TransactionDao transactionDao;

    public DepositService() {
        this.userDao = new UserDao();
        this.transactionDao = new TransactionDao();
    }

    public String deposit(String accountname, long amount) {
        if (amount <= 0) return "Invalid amount. Must be greater than 0.";

        try (Connection connection = DatabaseManager.getConnection()) {
            try {
                connection.setAutoCommit(false);
                
                User user = userDao.findByAccountnameForUpdate(connection, accountname);
                if (user == null) {
                    connection.rollback();
                    return "User not found.";
                }

                if (!(user instanceof org.example.model.user.Member)) {
                    connection.rollback();
                    return "Deposits are only available for members.";
                }

                userDao.addBalance(connection, accountname, amount);
                transactionDao.insertTransaction(connection, null, accountname, 0, null,
                        amount, null, "Deposit");
                
                connection.commit();
                FileLogger.info("User " + accountname + " deposited " + amount);
                return "SUCCESS";
            } catch (SQLException e) {
                try { connection.rollback(); } catch (SQLException ex) { /* Ignore */ }
                throw e;
            }
        } catch (SQLException e) {
            FileLogger.error("Deposit error for user " + accountname, e);
            return "Internal Error: " + e.getMessage();
        }
    }
}
