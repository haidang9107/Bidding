package org.example.server.service.finance;

import org.example.dto.response.BalanceResponse;
import org.example.model.user.User;
import org.example.model.user.Member;
import org.example.server.exception.FinanceException;
import org.example.server.exception.NotFoundException;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.TransactionDao;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service for handling withdrawal operations.
 */
public class WithdrawService {
    private final UserDao userDao;
    private final TransactionDao transactionDao;

    public WithdrawService() {
        this.userDao = new UserDao();
        this.transactionDao = new TransactionDao();
    }

    public BalanceResponse withdraw(String accountname, long amount) {
        if (amount <= 0) throw new FinanceException("Amount must be greater than 0");

        try (Connection connection = DatabaseManager.getConnection()) {
            try {
                connection.setAutoCommit(false);
                
                User user = userDao.findByAccountnameForUpdate(connection, accountname);
                if (user == null) {
                    throw new NotFoundException("User not found");
                }
                if (!(user instanceof Member member)) {
                    throw new FinanceException("Only members can withdraw funds");
                }

                long availableBalance = member.getBalance() - member.getBlockedBalance();
                if (availableBalance < amount) {
                    throw new FinanceException("Insufficient balance. Available: " + availableBalance, "FINANCE_ERROR");
                }

                userDao.addBalance(connection, accountname, -amount);
                transactionDao.insertTransaction(connection, accountname, null, 1, null,
                        amount, null, "Withdraw");
                
                connection.commit();
                FileLogger.info("User " + accountname + " withdrew " + amount);
                
                return new BalanceResponse(accountname, member.getBalance() - amount, member.getBlockedBalance());
            } catch (SQLException e) {
                try { connection.rollback(); } catch (SQLException ex) { /* Ignore */ }
                throw new FinanceException("Database error during withdrawal: " + e.getMessage());
            }
        } catch (SQLException e) {
            FileLogger.error("Withdrawal error for user " + accountname, e);
            throw new FinanceException("Internal server error during withdrawal");
        }
    }
}
