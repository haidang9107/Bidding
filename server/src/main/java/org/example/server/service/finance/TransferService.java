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
    private final org.example.server.event.EventPublisher eventPublisher;

    /**
     * Constructs a new TransferService.
     * @param txManager The transaction manager.
     * @param eventPublisher The event publisher.
     */
    public TransferService(TransactionManager txManager, org.example.server.event.EventPublisher eventPublisher) {
        this.userDao = UserDao.getInstance();
        this.transactionDao = TransactionDao.getInstance();
        this.txManager = txManager;
        this.eventPublisher = eventPublisher;
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

        java.util.List<BalanceResponse> results = txManager.execute(conn -> {
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
            
            if (toUser.getStatus() != 0) {
                throw new FinanceException("Recipient account is banned and cannot receive funds");
            }

            if (!(fromUser instanceof Member fromMember)) throw new FinanceException("Sender is not a member");
            if (!(toUser instanceof Member toMember)) throw new FinanceException("Recipient is not a member");

            long available = fromMember.getBalance() - fromMember.getBlockedBalance();
            if (available < amount) {
                throw new FinanceException("Insufficient funds. Available: " + available);
            }

            if (!userDao.addBalance(conn, fromAccount, -amount)) throw new FinanceException("Debit failed");
            if (!userDao.addBalance(conn, toAccount, amount)) throw new FinanceException("Credit failed");

            transactionDao.insertTransaction(conn, fromAccount, toAccount, 
                    TransactionType.TRANSFER, null, amount, null, "Transfer to " + toAccount);

            FileLogger.info("Transfer SUCCESS: " + fromAccount + " -> " + toAccount + " [" + amount + "]");
            
            return java.util.List.of(
                new BalanceResponse(fromAccount, fromMember.getBalance() - amount, fromMember.getBlockedBalance()),
                new BalanceResponse(toAccount, toMember.getBalance() + amount, toMember.getBlockedBalance())
            );
        });

        for (BalanceResponse res : results) {
            eventPublisher.publish(new org.example.server.event.BalanceChangedEvent(res.getAccountname(), res.getNewBalance(), res.getBlockedBalance()));
        }

        return results.get(0); // Return sender's balance
    }
}
