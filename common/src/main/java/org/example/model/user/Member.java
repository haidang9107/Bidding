package org.example.model.user;

import org.example.model.enums.UserRole;

/**
 * Represents a regular member who can both bid and sell.
 */
public class Member extends User {

    private long balance;
    private long blockedBalance;

    /**
     * Default constructor for Member.
     */
    public Member() {
        super();
        this.setRole(UserRole.MEMBER);
    }

    /**
     * Constructs a Member with all fields.
     * @param accountname The unique account name.
     * @param password The hashed password.
     * @param email The email address.
     * @param avt The avatar URL.
     * @param status The user status.
     * @param balance The current balance.
     * @param blockedBalance The current blocked balance.
     */
    public Member(String accountname, String password, String email, 
                  String avt, int status, long balance, long blockedBalance) {
        super(accountname, password, email, avt, UserRole.MEMBER, status);
        this.balance = balance;
        this.blockedBalance = blockedBalance;
    }

    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }

    public long getBlockedBalance() { return blockedBalance; }
    public void setBlockedBalance(long blockedBalance) { this.blockedBalance = blockedBalance; }
}
