package org.example.model.user;

import org.example.model.enums.UserRole;

/**
 * Represents an administrator in the system.
 */
public class Admin extends User {

    /**
     * Default constructor for Admin.
     */
    public Admin() {
        super();
        this.setRole(UserRole.ADMIN);
    }

    /**
     * Constructs an Admin with essential fields.
     * @param accountname The unique account name.
     * @param password The hashed password.
     * @param email The email address.
     * @param avt The avatar URL.
     * @param status The user status.
     */
    public Admin(String accountname, String password, String email, 
                 String avt, int status) {
        super(accountname, password, email, avt, UserRole.ADMIN, status);
    }
}
