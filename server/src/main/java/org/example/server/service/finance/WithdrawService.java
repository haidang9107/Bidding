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
 * Service for handling account withdrawals.
 */
public class WithdrawService {
    private final UserDao userDao;
    private final TransactionDao transactionDao;
    private final TransactionManager txManager;

    /**
     * Constructs a new WithdrawService.
     * @param txManager The transaction manager.
     */
    public WithdrawService(TransactionManager txManager) {
        this.userDao = new UserDao();
        this.transactionDao = new TransactionDao();
        this.txManager = txManager;
    }

    /**
     * Withdraws an amount from a user's account.
     * @param accountname The account name.
     * @param amount The amount to withdraw.
     * @return The updated balance response.
     */
    public BalanceResponse withdraw(String accountname, long amount) {
        if (amount <= 0) throw new FinanceException("Withdrawal amount must be positive");
        
        return txManager.execute(conn -> {
            User user = userDao.findByAccountnameForUpdate(conn, accountname);
            if (!(user instanceof Member member)) {
                throw new FinanceException("User not found or not a member.");
            }

            long available = member.getBalance() - member.getBlockedBalance();
            if (available < amount) {
                throw new FinanceException("Insufficient funds. Available: " + available);
            }

            boolean success = userDao.addBalance(conn, accountname, -amount);
            if (success) {
                transactionDao.insertTransaction(conn, accountname, null, 
                        TransactionType.WITHDRAW, null, amount, null, "Manual withdrawal");
                
                FileLogger.info("Withdrawal SUCCESS: User " + accountname + ", Amount " + amount);
                return new BalanceResponse(accountname, member.getBalance() - amount, member.getBlockedBalance());
            } else {
                throw new FinanceException("Withdrawal failed.");
            }
        });
    }
}
