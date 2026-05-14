package org.example.model.user;

import java.sql.Timestamp;

/**
 * Represents a user in the system.
 */
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

    /**
     * Default constructor for User.
     */
    public User() {
    }

    /**
     * Full constructor for User.
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

    /**
     * Gets the user ID.
     *
     * @return the user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID.
     *
     * @param userId the user ID to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the email address.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address.
     *
     * @param email the email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the phone number.
     *
     * @return the phone number
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Sets the phone number.
     *
     * @param phoneNumber the phone number to set
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Gets the gender.
     *
     * @return the gender
     */
    public String getGender() {
        return gender;
    }

    /**
     * Sets the gender.
     *
     * @param gender the gender to set
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * Gets the avatar URL or path.
     *
     * @return the avatar
     */
    public String getAvt() {
        return avt;
    }

    /**
     * Sets the avatar URL or path.
     *
     * @param avt the avatar to set
     */
    public void setAvt(String avt) {
        this.avt = avt;
    }

    /**
     * Gets the account balance.
     *
     * @return the balance
     */
    public double getBalance() {
        return balance;
    }

    /**
     * Sets the account balance.
     *
     * @param balance the balance to set
     */
    public void setBalance(double balance) {
        this.balance = balance;
    }

    /**
     * Gets the creation timestamp.
     *
     * @return the creation timestamp
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param createdAt the creation timestamp to set
     */
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
