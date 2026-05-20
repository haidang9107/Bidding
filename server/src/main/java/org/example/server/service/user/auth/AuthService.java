package org.example.server.service.user.auth;

import org.example.model.enums.Gender;
import org.example.model.enums.UserRole;
import org.example.model.user.Admin;
import org.example.model.user.Member;
import org.example.model.user.User;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Unified service for handling authentication-related tasks (Login, Signup).
 */
public class AuthService {
    private final UserDao userDao;

    public AuthService() {
        this.userDao = new UserDao();
    }

    /**
     * Authenticates a user.
     * @param username The username provided.
     * @param plainPassword The plain text password provided.
     * @return The User object if authenticated, null otherwise.
     */
    public User authenticate(String username, String plainPassword) {
        try (Connection conn = DatabaseManager.getConnection()) {
            User user = userDao.findByUsername(conn, username);
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
     * All new registrations are assigned the MEMBER role by default for security.
     * Admin roles must be assigned manually via database access.
     * @param username The desired username.
     * @param plainPassword The plain text password to be hashed.
     * @param email The user's email address.
     * @return true if registration was successful, false otherwise.
     */
    public boolean register(String username, String plainPassword, String email) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (userDao.findByUsername(conn, username) != null) {
                FileLogger.info("Registration failed: Username '" + username + "' already exists.");
                return false;
            }

            String hashedPassword = PasswordHashing.hashPassword(plainPassword);
            UserRole role = UserRole.MEMBER; // Secure default

            User newUser;
            Timestamp now = new Timestamp(System.currentTimeMillis());
            
            // All new users are Members. Admin is assigned manually in DB.
            newUser = new Member(0, username, hashedPassword, email, "", Gender.MALE, "", 0, 0, now);

            boolean success = userDao.createUser(conn, newUser);
            if (success) {
                FileLogger.info("User registered successfully as MEMBER: " + username);
            }
            return success;
        } catch (SQLException e) {
            FileLogger.error("Registration error for user: " + username, e);
        }
        return false;
    }
}
