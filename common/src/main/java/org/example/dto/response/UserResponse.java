package org.example.dto.response;

import org.example.model.enums.UserRole;
import org.example.model.user.User;
import org.example.model.user.Member;

/**
 * DTO for sending user information to the client without sensitive data like password.
 */
public class UserResponse {
    private String accountname;
    private String fullname;
    private String email;
    private String avt;
    private UserRole role;
    private int status;
    private Long balance; // Only for MEMBERS
    private Long blockedBalance; // Only for MEMBERS

    /**
     * Default constructor for UserResponse.
     */
    public UserResponse() {}

    /**
     * Constructs a UserResponse from a User object.
     * @param user the user to convert
     */
    public UserResponse(User user) {
        this.accountname = user.getAccountname();
        this.fullname = user.getFullname();
        this.email = user.getEmail();
        this.avt = user.getAvt();
        this.role = user.getRole();
        this.status = user.getStatus();
        
        if (user instanceof Member member) {
            this.balance = member.getBalance();
            this.blockedBalance = member.getBlockedBalance();
        }
    }

    // Getters and Setters
    /**
     * Gets the account name.
     * @return the account name
     */
    public String getAccountname() { return accountname; }

    /**
     * Sets the account name.
     * @param accountname the account name to set
     */
    public void setAccountname(String accountname) { this.accountname = accountname; }

    /**
     * Gets the full name.
     * @return the full name
     */
    public String getFullname() { return fullname; }

    /**
     * Sets the full name.
     * @param fullname the full name to set
     */
    public void setFullname(String fullname) { this.fullname = fullname; }

    /**
     * Gets the email.
     * @return the email
     */
    public String getEmail() { return email; }

    /**
     * Sets the email.
     * @param email the email to set
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Gets the avatar.
     * @return the avatar
     */
    public String getAvt() { return avt; }

    /**
     * Sets the avatar.
     * @param avt the avatar to set
     */
    public void setAvt(String avt) { this.avt = avt; }

    /**
     * Gets the user role.
     * @return the role
     */
    public UserRole getRole() { return role; }

    /**
     * Sets the user role.
     * @param role the role to set
     */
    public void setRole(UserRole role) { this.role = role; }

    /**
     * Gets the user status.
     * @return the status
     */
    public int getStatus() { return status; }

    /**
     * Sets the user status.
     * @param status the status to set
     */
    public void setStatus(int status) { this.status = status; }

    /**
     * Gets the user balance (for Members).
     * @return the balance
     */
    public Long getBalance() { return balance; }

    /**
     * Sets the user balance (for Members).
     * @param balance the balance to set
     */
    public void setBalance(Long balance) { this.balance = balance; }

    /**
     * Gets the blocked balance (for Members).
     * @return the blocked balance
     */
    public Long getBlockedBalance() { return blockedBalance; }

    /**
     * Sets the blocked balance (for Members).
     * @param blockedBalance the blocked balance to set
     */
    public void setBlockedBalance(Long blockedBalance) { this.blockedBalance = blockedBalance; }
}
