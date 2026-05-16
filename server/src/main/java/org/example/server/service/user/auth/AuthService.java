package org.example.server.service.user.auth;

import org.example.model.user.User;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Unified service for handling authentication-related tasks (Login, Signup).
 */
public class AuthService {
    private final UserDao userDao;

    public AuthService(UserDao userDao) {
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
            if (user != null && PasswordHashing.checkPassword(plainPassword, user.getPassword())) {
                FileLogger.info("User authenticated: " + username);
                return user;
            }
        } catch (SQLException e) {
            FileLogger.error("Authentication error for user: " + username, e);
        }
        return null;
    }

    /**
     * Registers a new user in the system.
     * @param username The desired username.
     * @param plainPassword The plain text password to be hashed.
     * @param email The user's email address.
     * @param role The role assigned to the user.
     * @return true if registration was successful, false otherwise.
     */
    public boolean register(String username, String plainPassword, String email, String role) {
        try {
            if (userDao.findByUsername(username) != null) {
                FileLogger.info("Registration failed: Username '" + username + "' already exists.");
                return false;
            }

            String hashedPassword = PasswordHashing.hashPassword(plainPassword);
            User newUser = new User(
                UUID.randomUUID().toString(),
                username,
                hashedPassword,
                email,
                "", // phonenumber
                "", // gender
                "", // avt
                0.0, // balance
                new Timestamp(System.currentTimeMillis())
            );

            boolean success = userDao.createUser(newUser, role);
            if (success) {
                FileLogger.info("User registered successfully: " + username);
            }
            return success;
        } catch (SQLException e) {
            FileLogger.error("Registration error for user: " + username, e);
        }
        return false;
    }
}
