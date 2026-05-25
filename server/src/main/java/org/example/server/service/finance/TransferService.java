package org.example.server.service.finance;

import org.example.dto.response.BalanceResponse;
import org.example.model.user.Member;
import org.example.model.user.User;
import org.example.model.enums.TransactionType;
import org.example.server.exception.FinanceException;
import org.example.server.repository.TransactionDao;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

/**
 * Service for handling fund transfers between users.
 */
public class TransferService {
    private final UserDao userDao;
    private final TransactionDao transactionDao;
    private final TransactionManager txManager;

    /**
     * Constructs a new TransferService.
     * @param txManager The transaction manager.
     */
    public TransferService(TransactionManager txManager) {
        this.userDao = new UserDao();
        this.transactionDao = new TransactionDao();
        this.txManager = txManager;
    }

    /**
     * Transfers funds between two member accounts.
     * @param fromAccount The sender's account name.
     * @param toAccount The receiver's account name.
     * @param amount The amount to transfer.
     * @return The sender's updated balance.
     */
    public BalanceResponse transfer(String fromAccount, String toAccount, long amount) {
        if (amount <= 0) throw new FinanceException("Transfer amount must be positive");
        if (fromAccount.equals(toAccount)) throw new FinanceException("Cannot transfer to yourself");

        return txManager.execute(conn -> {
            // Lock in consistent order to avoid deadlocks
            if (fromAccount.compareTo(toAccount) < 0) {
                userDao.findByAccountnameForUpdate(conn, fromAccount);
                userDao.findByAccountnameForUpdate(conn, toAccount);
            } else {
                userDao.findByAccountnameForUpdate(conn, toAccount);
                userDao.findByAccountnameForUpdate(conn, fromAccount);
            }

            User fromUser = userDao.findByAccountname(conn, fromAccount);
            User toUser = userDao.findByAccountname(conn, toAccount);
            if (fromUser == null) throw new FinanceException("Sender not found");
            if (toUser == null) throw new FinanceException("Recipient not found");
            
            if (!(fromUser instanceof Member fromMember)) throw new FinanceException("Sender is not a member");
            if (!(toUser instanceof Member)) throw new FinanceException("Recipient is not a member");

            long available = fromMember.getBalance() - fromMember.getBlockedBalance();
            if (available < amount) {
                throw new FinanceException("Insufficient funds. Available: " + available);
            }

            if (!userDao.addBalance(conn, fromAccount, -amount)) throw new FinanceException("Debit failed");
            if (!userDao.addBalance(conn, toAccount, amount)) throw new FinanceException("Credit failed");

            transactionDao.insertTransaction(conn, fromAccount, toAccount, 
                    TransactionType.TRANSFER, null, amount, null, "Transfer to " + toAccount);

            User updatedFromUser = userDao.findByAccountname(conn, fromAccount);
            if (updatedFromUser instanceof Member updatedFromMember) {
                FileLogger.info("Transfer SUCCESS: " + fromAccount + " -> " + toAccount + " [" + amount + "]");
                return new BalanceResponse(fromAccount, updatedFromMember.getBalance(), updatedFromMember.getBlockedBalance());
            }
            throw new FinanceException("Transfer failed during finalization");
        });
    }
}
