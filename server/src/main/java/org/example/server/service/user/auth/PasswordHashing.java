package org.example.server.service.user.auth;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for password hashing and verification using BCrypt.
 */
public class PasswordHashing {

    /**
     * Hashes a plain text password using BCrypt with a work factor of 12.
     * @param plainTextPassword The plain text password to hash.
     * @return The hashed password string.
     */
    public static String hashPassword(String plainTextPassword) {
        // gensalt() automatically generates a secure random salt string
        // Parameter 12 is the work factor, default is 10
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12));
    }

    /**
     * Checks if a plain text password matches a hashed password.
     * @param plainTextPassword The plain text password to check.
     * @param hashedFromDB The hashed password retrieved from the database.
     * @return true if the passwords match, false otherwise.
     */
    public static boolean checkPassword(String plainTextPassword, String hashedFromDB) {
        // The algorithm automatically extracts the Salt from hashedFromDB for comparison
        return BCrypt.checkpw(plainTextPassword, hashedFromDB);
    }
}
