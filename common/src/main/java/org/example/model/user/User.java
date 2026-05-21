package org.example.model.user;

import org.example.model.enums.UserRole;

/**
 * Represents an abstract user in the system.
 */
public abstract class User {

    private String accountname;
    private String fullname;
    private String password;
    private String email;
    private String avt;
    private UserRole role;
    private int status; // 0: ACTIVE, 1: BANNED

    public User() {
    }

    public User(String accountname, String password, String email, 
                String avt, UserRole role, int status) {
        this.accountname = accountname;
        this.fullname = accountname;
        this.password = password;
        this.email = email;
        this.avt = avt;
        this.role = role;
        this.status = status;
    }

    // Getters and Setters
    public String getAccountname() { return accountname; }
    public void setAccountname(String accountname) { this.accountname = accountname; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvt() { return avt; }
    public void setAvt(String avt) { this.avt = avt; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
