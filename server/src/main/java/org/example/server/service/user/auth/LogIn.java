package org.example.server.service.user.auth;

import org.example.model.user.User;
import org.example.server.repository.UserDao;
import java.sql.SQLException;

public class LogIn {
    private final UserDao userDao;

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
                    System.out.println(">>> Authentication successful for user: " + username);
                    return user;
                } else {
                    System.out.println(">>> Authentication failed: Incorrect password for " + username);
                }
            } else {
                System.out.println(">>> Authentication failed: User not found: " + username);
            }
        } catch (SQLException e) {
            System.err.println(">>> Database error during authentication: " + e.getMessage());
        }
        return null;
    }
}
