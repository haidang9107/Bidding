package org.example.server.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Thực thể đại diện cho người dùng trong hệ thống đấu giá.
 * Kế thừa từ Entity để lấy thuộc tính createdAt.
 */
public class User extends Entity {
    private String userId;
    private String username;
    private String password;
    private String email;
    private String phoneNumber;
    private String gender; // Có thể dùng Enum 'MALE', 'FEMALE', 'OTHER'
    private String avt;
    private BigDecimal balance;
    private String role; // Có thể dùng Enum 'ADMIN', 'USER', 'SELLER'

    // Constructor mặc định
    public User() {
        super();
    }

    // Constructor đầy đủ tham số (bao gồm cả createdAt từ lớp cha)
    public User(String userId, String username, String password, String email,
                String phoneNumber, String gender, String avt,
                BigDecimal balance, String role, Timestamp createdAt) {
        super(createdAt);
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.avt = avt;
        this.balance = balance;
        this.role = role;
    }

    // --- Getter và Setter ---

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAvt() {
        return avt;
    }

    public void setAvt(String avt) {
        this.avt = avt;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", balance=" + balance +
                ", createdAt=" + createdAt +
                '}';
    }
}