package org.example.server.service.finance;

import org.example.dto.response.BalanceResponse;
import org.example.model.user.Member;
import org.example.model.user.User;
import org.example.server.exception.FinanceException;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

/**
 * Service for handling fund withdrawals.
 */
public class WithdrawService {
    private final UserDao userDao;
    private final TransactionManager txManager;

    /**
     * Constructs a WithdrawService with dependencies.
     * @param txManager The transaction manager.
     */
    public WithdrawService(TransactionManager txManager) {
        this.userDao = new UserDao();
        this.txManager = txManager;
    }

    /**
     * Withdraws an amount from a user's account.
     * @param accountname The account name.
     * @param amount The amount to withdraw.
     * @return The updated balance.
     */
    public BalanceResponse withdraw(String accountname, long amount) {
        if (amount <= 0) throw new FinanceException("Withdraw amount must be positive");

        return txManager.execute(conn -> {
            User user = userDao.findByAccountnameForUpdate(conn, accountname);
            if (user == null) throw new FinanceException("User not found");
            if (!(user instanceof Member member)) throw new FinanceException("User is not a member");

            long available = member.getBalance() - member.getBlockedBalance();
            if (available < amount) {
                throw new FinanceException("Insufficient funds. Available: " + available);
            }

            boolean success = userDao.addBalance(conn, accountname, -amount);
            if (success) {
                User updatedUser = userDao.findByAccountname(conn, accountname);
                if (updatedUser instanceof Member updatedMember) {
                    FileLogger.info("Withdraw SUCCESS: User " + accountname + ", Amount " + amount);
                    return new BalanceResponse(accountname, updatedMember.getBalance(), updatedMember.getBlockedBalance());
                }
            }
            throw new FinanceException("Withdraw failed.");
        });
    }
}
