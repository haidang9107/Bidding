package org.example.server.service.finance;

import org.example.model.Transaction;
import org.example.server.repository.TransactionDao;
import org.example.server.repository.TransactionManager;

import java.util.List;

/**
 * Service for handling transaction history retrieval.
 */
public class TransactionService {
    private final TransactionDao transactionDao;
    private final TransactionManager txManager;

    /**
     * Constructs a new TransactionService.
     * @param txManager The transaction manager.
     */
    public TransactionService(TransactionManager txManager) {
        this.transactionDao = new TransactionDao();
        this.txManager = txManager;
    }

    /**
     * Retrieves all transactions for a specific user.
     * @param accountname The account name.
     * @return A list of transactions.
     */
    public List<Transaction> getTransactions(String accountname) {
        return txManager.query(conn -> transactionDao.getTransactionsByUser(conn, accountname));
    }
}
