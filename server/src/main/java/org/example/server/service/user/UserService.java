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
        this.userDao = UserDao.getInstance();
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

    /**
     * Updates the full name of a user.
     * @param accountname The account name of the user.
     * @param fullname The new full name.
     * @return True if successful.
     */
    public boolean updateFullname(String accountname, String fullname) {
        return txManager.execute(conn -> userDao.updateFullname(conn, accountname, fullname));
    }

    /**
     * Securely updates the user's password.
     * @param accountname The account name.
     * @param oldPassword The current plain text password.
     * @param newPassword The new plain text password.
     * @return True if successful.
     * @throws org.example.server.exception.AuthException if old password is incorrect.
     */
    public boolean updatePassword(String accountname, String oldPassword, String newPassword) {
        return txManager.execute(conn -> {
            org.example.model.user.User user = userDao.findByAccountname(conn, accountname);
            if (user == null) return false;

            // 1. Verify old password
            if (!org.example.server.service.user.auth.PasswordHashing.checkPassword(oldPassword, user.getPassword())) {
                throw new org.example.server.exception.AuthException("Mật khẩu cũ không chính xác");
            }

            // 2. Hash new password
            String newHashed = org.example.server.service.user.auth.PasswordHashing.hashPassword(newPassword);

            // 3. Save to DB
            return userDao.updatePassword(conn, accountname, newHashed);
        });
    }
}
