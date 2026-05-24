package org.example.dto.response;

/**
 * Data Transfer Object for user balance information.
 */
public class BalanceResponse {
    private String accountname;
    private long newBalance;
    private long blockedBalance;

    /**
     * Default constructor for BalanceResponse.
     */
    public BalanceResponse() {}

    /**
     * Constructs a BalanceResponse with specified details.
     * @param accountname the account name of the user
     * @param newBalance the new balance of the user
     * @param blockedBalance the amount of funds currently blocked
     */
    public BalanceResponse(String accountname, long newBalance, long blockedBalance) {
        this.accountname = accountname;
        this.newBalance = newBalance;
        this.blockedBalance = blockedBalance;
    }

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
     * Gets the new balance.
     * @return the new balance
     */
    public long getNewBalance() { return newBalance; }

    /**
     * Sets the new balance.
     * @param newBalance the new balance to set
     */
    public void setNewBalance(long newBalance) { this.newBalance = newBalance; }

    /**
     * Gets the blocked balance.
     * @return the blocked balance
     */
    public long getBlockedBalance() { return blockedBalance; }

    /**
     * Sets the blocked balance.
     * @param blockedBalance the blocked balance to set
     */
    public void setBlockedBalance(long blockedBalance) { this.blockedBalance = blockedBalance; }
}
