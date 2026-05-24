package org.example.server.service.finance;

import org.example.dto.response.PagedResponse;
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
     * Retrieves a paged list of transactions for a specific user.
     * @param accountname The account name.
     * @param page The page number (1-based).
     * @param pageSize The number of items per page.
     * @return A paged response containing transactions.
     */
    public PagedResponse<Transaction> getTransactionsPaged(String accountname, int page, int pageSize) {
        return txManager.query(conn -> {
            long totalItems = transactionDao.getTotalTransactionsCount(conn, accountname);
            List<Transaction> transactions = transactionDao.getTransactionsPaged(conn, accountname, pageSize, (page - 1) * pageSize);
            return new PagedResponse<>(transactions, totalItems, page, pageSize);
        });
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
