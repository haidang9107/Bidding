package org.example.server.service.user;

import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;

/**
 * Service for common user profile operations.
 * Refactored to use TransactionManager.
 */
public class UserService {
    private final UserDao userDao;
    private final TransactionManager txManager;

    /**
     * Constructs a new UserService.
     * @param txManager The transaction manager to use for database operations.
     */
    public UserService(TransactionManager txManager) {
        this.userDao = new UserDao();
        this.txManager = txManager;
    }

    /**
     * Updates the avatar of a user.
     * @param accountname The account name of the user.
     * @param avatarPath The new avatar path or URL.
     * @return True if successful.
     */
    public boolean updateAvatar(String accountname, String avatarPath) {
        return txManager.execute(conn -> userDao.updateAvatar(conn, accountname, avatarPath));
    }

    /**
     * Updates the email of a user.
     * @param accountname The account name of the user.
     * @param email The new email address.
     * @return True if successful.
     */
    public boolean updateEmail(String accountname, String email) {
        return txManager.execute(conn -> userDao.updateEmail(conn, accountname, email));
    }
}
