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
     * @param accountname The accountname provided.
     * @param plainPassword The plain text password provided.
     * @return The User object if authenticated, null otherwise.
     */
    public User authenticate(String accountname, String plainPassword) throws org.example.server.exception.AuthException {
        try (Connection conn = DatabaseManager.getConnection()) {
            User user = userDao.findByAccountname(conn, accountname);
            if (user != null && PasswordHashing.checkPassword(plainPassword, user.getPassword())) {
                if (user.getStatus() == 1) {
                    throw new org.example.server.exception.AuthException("Your account has been BANNED.");
                }
                FileLogger.info("User authenticated: " + accountname);
                // Security: Remove sensitive password hash before returning
                user.setPassword(null);
                return user;
            }
        } catch (SQLException e) {
            FileLogger.error("Authentication error for user: " + accountname, e);
            throw new org.example.server.exception.AuthException("Internal server error during authentication");
        }
        return null;
    }

    /**
     * Registers a new user in the system.
     */
    public boolean register(String accountname, String plainPassword, String email) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (userDao.findByAccountname(conn, accountname) != null) {
                FileLogger.info("Registration failed: Account name '" + accountname + "' already exists.");
                return false;
            }

            String hashedPassword = PasswordHashing.hashPassword(plainPassword);
            User newUser = new Member(accountname, hashedPassword, email, null, 0, 0, 0);

            boolean success = userDao.createUser(conn, newUser);
            if (success) {
                FileLogger.info("User registered successfully as MEMBER: " + accountname);
            }
            return success;
        } catch (SQLException e) {
            FileLogger.error("Registration error for user: " + accountname, e);
        }
        return false;
    }

    /**
     * Checks if a user has permission to perform a specific action.
     * Centralizes RBAC logic in the service layer.
     */
    public boolean canAccess(org.example.model.enums.MessageType type, User user) {
        // Public routes
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
            case USER_UPDATE_AVATAR, UPDATE_PROFILE, GET_PROFILE, PRODUCT_DETAIL, PRODUCT_LIST -> 
                true; 
            default -> true; 
        };
    }
}
