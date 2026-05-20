package org.example.model.user;

import org.example.model.enums.Gender;
import org.example.model.enums.UserRole;
import java.sql.Timestamp;

/**
 * Represents an abstract user in the system.
 */
public abstract class User {

    private int userId;
    private String username;
    private String password;
    private String email;
    private String phoneNumber;
    private Gender gender;
    private String avt;
    private long balance;
    private long blockedBalance;
    private UserRole role;
    private Timestamp createdAt;

    public User() {
    }

    public User(int userId, String username, String password, String email, 
                String phoneNumber, Gender gender, String avt, long balance, 
                long blockedBalance, UserRole role, Timestamp createdAt) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.avt = avt;
        this.balance = balance;
        this.blockedBalance = blockedBalance;
        this.role = role;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public String getAvt() { return avt; }
    public void setAvt(String avt) { this.avt = avt; }

    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }

    public long getBlockedBalance() { return blockedBalance; }
    public void setBlockedBalance(long blockedBalance) { this.blockedBalance = blockedBalance; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
