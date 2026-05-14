package org.example.server.service.user.auth;

import org.example.model.user.User;
import org.example.server.repository.UserDao;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Service class for handling user registration (Sign Up).
 */
public class SignUp {
    private final UserDao userDao;

    /**
     * Constructs a SignUp service with the specified UserDao.
     * @param userDao The data access object for users.
     */
    public SignUp(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Registers a new user in the system.
     * @param username The desired username.
     * @param plainPassword The plain text password to be hashed.
     * @param email The user's email address.
     * @param role The role assigned to the user (e.g., ADMIN, SELLER, BIDDER).
     * @return true if registration was successful, false if the username exists or an error occurs.
     */
    public boolean register(String username, String plainPassword, String email, String role) {
        try {
            // Check if user already exists
            if (userDao.findByUsername(username) != null) {
                return false;
            }

            // Hash the password
            String hashedPassword = PasswordHashing.hashPassword(plainPassword);

            // Create new User object
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

            return userDao.createUser(newUser, role);
        } catch (SQLException e) {
            // Error logged by DatabaseManager or caller
        }
        return false;
    }
}
