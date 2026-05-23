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
 * Service for handling money transfers between users.
 */
public class TransferService {
    private final UserDao userDao;
    private final TransactionDao transactionDao;

    public TransferService() {
        this.userDao = new UserDao();
        this.transactionDao = new TransactionDao();
    }

    public BalanceResponse transfer(String fromAccount, String toAccount, long amount) {
        if (fromAccount.equals(toAccount)) throw new FinanceException("Cannot transfer to yourself");
        if (amount <= 0) throw new FinanceException("Amount must be greater than 0");

        try (Connection connection = DatabaseManager.getConnection()) {
            try {
                connection.setAutoCommit(false);

                String firstAccount = fromAccount.compareTo(toAccount) < 0 ? fromAccount : toAccount;
                String secondAccount = fromAccount.compareTo(toAccount) < 0 ? toAccount : fromAccount;

                userDao.findByAccountnameForUpdate(connection, firstAccount);
                userDao.findByAccountnameForUpdate(connection, secondAccount);

                User fromUser = userDao.findByAccountname(connection, fromAccount);
                User toUser = userDao.findByAccountname(connection, toAccount);

                if (fromUser == null || toUser == null) {
                    throw new NotFoundException("One or more users not found");
                }
                if (!(fromUser instanceof Member fromMember) || !(toUser instanceof Member toMember)) {
                    throw new FinanceException("Transfers are only available between members");
                }

                long availableBalance = fromMember.getBalance() - fromMember.getBlockedBalance();
                if (availableBalance < amount) {
                    throw new FinanceException("Insufficient balance", "FINANCE_ERROR");
                }

                userDao.addBalance(connection, fromAccount, -amount);
                userDao.addBalance(connection, toAccount, amount);
                transactionDao.insertTransaction(connection, fromAccount, toAccount, 2,
                        null, amount, null, "Transfer");

                connection.commit();
                FileLogger.info("Transfer: " + amount + " from " + fromAccount + " to " + toAccount);
                
                return new BalanceResponse(fromAccount, fromMember.getBalance() - amount, fromMember.getBlockedBalance());

            } catch (SQLException e) {
                try { connection.rollback(); } catch (SQLException ex) { /* Ignore */ }
                throw new FinanceException("Database error during transfer: " + e.getMessage());
            }
        } catch (SQLException e) {
            FileLogger.error("Transfer error from " + fromAccount + " to " + toAccount, e);
            throw new FinanceException("Internal server error during transfer");
        }
    }
}
