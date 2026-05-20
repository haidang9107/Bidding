package org.example.model.user;

import org.example.model.enums.Gender;
import org.example.model.enums.UserRole;
import java.sql.Timestamp;

/**
 * Represents an administrator in the system.
 */
public class Admin extends User {

    public Admin() {
        super();
        this.setRole(UserRole.ADMIN);
    }

    public Admin(int userId, String username, String password, String email, 
                 String phoneNumber, Gender gender, String avt, long balance, 
                 long blockedBalance, Timestamp createdAt) {
        super(userId, username, password, email, phoneNumber, gender, avt, 
              balance, blockedBalance, UserRole.ADMIN, createdAt);
    }
    
    // Admin specific methods (e.g., banUser, deleteAuction)
}
