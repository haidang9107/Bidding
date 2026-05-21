package org.example.server.service.user;

import org.example.server.repository.DatabaseManager;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service for common user profile operations.
 */
public class UserService {
    private final UserDao userDao;

    public UserService() {
        this.userDao = new UserDao();
    }

    /**
     * Updates the avatar of a user.
     */
    public boolean updateAvatar(String accountname, String avatarPath) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return userDao.updateAvatar(conn, accountname, avatarPath);
        }
    }
}
