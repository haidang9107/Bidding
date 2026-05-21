package org.example.dto;

import org.example.model.enums.UserRole;
import org.example.model.user.User;
import org.example.model.user.Member;

/**
 * DTO for sending user information to the client without sensitive data like password.
 */
public class UserResponse {
    private String accountname;
    private String email;
    private String avt;
    private UserRole role;
    private int status;
    private Long balance; // Only for MEMBERS
    private Long blockedBalance; // Only for MEMBERS

    public UserResponse() {}

    public UserResponse(User user) {
        this.accountname = user.getAccountname();
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
    public String getAccountname() { return accountname; }
    public void setAccountname(String accountname) { this.accountname = accountname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvt() { return avt; }
    public void setAvt(String avt) { this.avt = avt; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public Long getBalance() { return balance; }
    public void setBalance(Long balance) { this.balance = balance; }

    public Long getBlockedBalance() { return blockedBalance; }
    public void setBlockedBalance(Long blockedBalance) { this.blockedBalance = blockedBalance; }
}
