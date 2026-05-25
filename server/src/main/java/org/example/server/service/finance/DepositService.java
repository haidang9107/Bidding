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
 * Service for handling account deposits.
 */
public class DepositService {
    private final UserDao userDao;
    private final TransactionDao transactionDao;
    private final TransactionManager txManager;

    /**
     * Constructs a new DepositService.
     * @param txManager The transaction manager.
     */
    public DepositService(TransactionManager txManager) {
        this.userDao = new UserDao();
        this.transactionDao = new TransactionDao();
        this.txManager = txManager;
    }

    /**
     * Deposits an amount into a user's account.
     * @param accountname The account name.
     * @param amount The amount to deposit.
     * @return The updated balance response.
     */
    public BalanceResponse deposit(String accountname, long amount) {
        if (amount <= 0) throw new FinanceException("Deposit amount must be positive");
        
        return txManager.execute(conn -> {
            boolean success = userDao.addBalance(conn, accountname, amount);
            if (success) {
                User user = userDao.findByAccountname(conn, accountname);
                if (user instanceof Member member) {
                    transactionDao.insertTransaction(conn, null, accountname, 
                            TransactionType.DEPOSIT, null, amount, null, "Manual deposit");
                    
                    FileLogger.info("Deposit SUCCESS: User " + accountname + ", Amount " + amount);
                    return new BalanceResponse(accountname, member.getBalance(), member.getBlockedBalance());
                }
                throw new FinanceException("User is not a member.");
            } else {
                throw new FinanceException("Deposit failed. User not found.");
            }
        });
    }
}
