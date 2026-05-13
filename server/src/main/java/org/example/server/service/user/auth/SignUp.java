package org.example.server.service.user.auth;

import org.example.model.user.User;
import org.example.server.repository.UserDao;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

public class SignUp {
    private final UserDao userDao;

    public SignUp(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Registers a new user.
     * @param username
     * @param plainPassword
     * @param email
     * @param role (ADMIN, SELLER, BIDDER)
     * @return true if successful, false otherwise.
     */
    public boolean register(String username, String plainPassword, String email, String role) {
        try {
            // Check if user already exists
            if (userDao.findByUsername(username) != null) {
                System.out.println(">>> Registration failed: Username already exists: " + username);
                return false;
            }

            // Hash the password
            String hashedPassword = PasswordHashing.hashPassword(plainPassword);

            // Create new User object (assuming Bidder by default if role is not specified, but here we pass role)
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
            System.err.println(">>> Database error during registration: " + e.getMessage());
        }
        return false;
    }
}
