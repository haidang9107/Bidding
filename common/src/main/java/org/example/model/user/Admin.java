package org.example.model.user;

import org.example.model.enums.UserRole;

/**
 * Represents an administrator in the system.
 */
public class Admin extends User {

    public Admin() {
        super();
        this.setRole(UserRole.ADMIN);
    }

    public Admin(String accountname, String password, String email, 
                 String avt, int status) {
        super(accountname, password, email, avt, UserRole.ADMIN, status);
    }
}
