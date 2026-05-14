package org.example.server.service.user.auth;

import org.example.model.user.User;
import org.example.server.repository.UserDao;
import java.sql.SQLException;

/**
 * Service class for handling user login and authentication.
 */
public class LogIn {
    private final UserDao userDao;

    /**
     * Constructs a LogIn service with the specified UserDao.
     * @param userDao The data access object for users.
     */
    public LogIn(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Authenticates a user.
     * @param username The username provided.
     * @param plainPassword The plain text password provided.
     * @return The User object if authenticated, null otherwise.
     */
    public User authenticate(String username, String plainPassword) {
        try {
            User user = userDao.findByUsername(username);
            if (user != null) {
                if (PasswordHashing.checkPassword(plainPassword, user.getPassword())) {
                    return user;
                } else {
                }
            } else {
            }
        } catch (SQLException e) {
        }
        return null;
    }
}
