package org.example.model.user;

import java.sql.Timestamp;

public class User {

    // =========================
    // Fields
    // =========================
    private String userId;

    private String username;

    private String password;

    private String email;

    private String phoneNumber;

    private String gender;

    private String avt;

    private double balance;

    private Timestamp createdAt;

    // =========================
    // Constructor rỗng
    // =========================
    public User() {
    }

    // =========================
    // Constructor đầy đủ
    // =========================
    public User(String userId,
                String username,
                String password,
                String email,
                String phoneNumber,
                String gender,
                String avt,
                double balance,
                Timestamp createdAt) {

        this.userId = userId;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.avt = avt;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    // =========================
    // Getter & Setter
    // =========================

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // -------------------------

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // -------------------------

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // -------------------------

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // -------------------------

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // -------------------------

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    // -------------------------

    public String getAvt() {
        return avt;
    }

    public void setAvt(String avt) {
        this.avt = avt;
    }

    // -------------------------

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    // -------------------------

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}