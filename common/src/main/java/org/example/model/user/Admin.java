package org.example.model.user;

import java.sql.Timestamp;

/**
 * Represents an administrator in the system.
 */
public class Admin extends User {

    /**
     * Default constructor for Admin.
     */
    public Admin() {
        super();
    }

    /**
     * Full constructor for Admin.
     *
     * @param userId the unique identifier for the user
     * @param username the username of the user
     * @param password the password of the user
     * @param email the email of the user
     * @param phoneNumber the phone number of the user
     * @param gender the gender of the user
     * @param avt the avatar URL or path of the user
     * @param balance the account balance of the user
     * @param createdAt the timestamp when the user was created
     */
    public Admin(String userId,
                 String username,
                 String password,
                 String email,
                 String phoneNumber,
                 String gender,
                 String avt,
                 double balance,
                 Timestamp createdAt) {

        super(
                userId,
                username,
                password,
                email,
                phoneNumber,
                gender,
                avt,
                balance,
                createdAt
        );
    }
}
