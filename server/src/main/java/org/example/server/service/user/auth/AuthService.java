package org.example.server.service.user.auth;

import org.example.model.user.Member;
import org.example.model.user.User;
import org.example.server.exception.AuthException;
import org.example.server.exception.ValidationException;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;

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
     * @param accountname The accountname provided.
     * @param plainPassword The plain text password provided.
     * @return The User object if authenticated.
     */
    public User authenticate(String accountname, String plainPassword) {
        try (Connection conn = DatabaseManager.getConnection()) {
            User user = userDao.findByAccountname(conn, accountname);
            if (user != null && PasswordHashing.checkPassword(plainPassword, user.getPassword())) {
                if (user.getStatus() == 1) {
                    throw new AuthException("Your account has been BANNED.");
                }
                FileLogger.info("User authenticated: " + accountname);
                user.setPassword(null); // Security
                return user;
            }
            throw new AuthException("Invalid account name or password");
        } catch (SQLException e) {
            FileLogger.error("Authentication error for user: " + accountname, e);
            throw new AuthException("Internal server error during authentication");
        }
    }

    /**
     * Registers a new user in the system.
     */
    public void register(String accountname, String plainPassword, String email) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (userDao.findByAccountname(conn, accountname) != null) {
                throw new ValidationException("Account name '" + accountname + "' already exists.");
            }

            String hashedPassword = PasswordHashing.hashPassword(plainPassword);
            User newUser = new Member(accountname, hashedPassword, email, null, 0, 0, 0);

            boolean success = userDao.createUser(conn, newUser);
            if (!success) {
                throw new AuthException("Failed to create user account");
            }
            FileLogger.info("User registered successfully: " + accountname);
        } catch (SQLException e) {
            FileLogger.error("Registration error for user: " + accountname, e);
            throw new AuthException("Internal error during registration");
        }
    }

    /**
     * Checks if a user has permission to perform a specific action.
     */
    public boolean canAccess(org.example.model.enums.MessageType type, User user) {
        if (type == org.example.model.enums.MessageType.LOGIN || 
            type == org.example.model.enums.MessageType.SIGNUP || 
            type == org.example.model.enums.MessageType.PING) {
            return true;
        }
        
        if (user == null) return false;

        return switch (type) {
            case ADMIN_GET_ALL_USERS, ADMIN_BAN_USER, ADMIN_CANCEL_AUCTION -> 
                user.getRole() == org.example.model.enums.UserRole.ADMIN;
            case BID_PLACE, AUTO_BID_SET, AUTO_BID_CANCEL, PRODUCT_ADD, DEPOSIT, WITHDRAW, TRANSFER,
                 JOIN_AUCTION_ROOM, LEAVE_AUCTION_ROOM -> 
                user.getRole() == org.example.model.enums.UserRole.MEMBER;
            default -> true; 
        };
    }
}
