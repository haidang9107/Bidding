package org.example.server.service.user.auth;

import org.example.model.user.Member;
import org.example.model.user.User;
import org.example.server.exception.AuthException;
import org.example.server.exception.ValidationException;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;
import org.example.util.FileLogger;

/**
 * Unified service for handling authentication-related tasks (Login, Signup).
 * Refactored to use TransactionManager.
 */
public class AuthService {
    private final UserDao userDao;
    private final TransactionManager txManager;

    /**
     * Constructs a new AuthService.
     * @param txManager The transaction manager to use for database operations.
     */
    public AuthService(TransactionManager txManager) {
        this.userDao = new UserDao();
        this.txManager = txManager;
    }

    /**
     * Authenticates a user by account name and password.
     * @param accountname The account name.
     * @param plainPassword The plain text password.
     * @return The authenticated user object (with password removed).
     * @throws AuthException If authentication fails or account is banned.
     */
    public User authenticate(String accountname, String plainPassword) {
        return txManager.query(conn -> {
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
        });
    }

    /**
     * Registers a new member user.
     * @param accountname The desired account name.
     * @param plainPassword The plain text password.
     * @param email The email address.
     * @throws ValidationException If the account name already exists.
     * @throws AuthException If registration fails.
     */
    public void register(String accountname, String plainPassword, String email) {
        txManager.run(conn -> {
            if (userDao.findByAccountname(conn, accountname) != null) {
                throw new ValidationException("Account name '" + accountname + "' already exists.");
            }

            String hashedPassword = PasswordHashing.hashPassword(plainPassword);
            User newUser = new Member(accountname, hashedPassword, email, null, 0, 0, 0);

            if (!userDao.createUser(conn, newUser)) {
                throw new AuthException("Failed to create user account");
            }
            FileLogger.info("User registered successfully: " + accountname);
        });
    }

    /**
     * Checks if a user has permission to perform a specific action based on message type.
     * @param type The message type representing the action.
     * @param user The user performing the action.
     * @return True if access is granted.
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
            case BID_PLACE, AUTO_BID_SET, AUTO_BID_CANCEL, BID_HISTORY, PRODUCT_ADD,
                 PRODUCT_CREATE, MY_PRODUCT_LIST, AUCTION_OPEN,
                 DEPOSIT, WITHDRAW, TRANSFER,
                 JOIN_AUCTION_ROOM, LEAVE_AUCTION_ROOM -> 
                user.getRole() == org.example.model.enums.UserRole.MEMBER;
            default -> true; 
        };
    }
}
