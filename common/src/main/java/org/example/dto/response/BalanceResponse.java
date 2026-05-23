package org.example.dto.response;

public class BalanceResponse {
    private String accountname;
    private long newBalance;
    private long blockedBalance;

    public BalanceResponse() {}

    public BalanceResponse(String accountname, long newBalance, long blockedBalance) {
        this.accountname = accountname;
        this.newBalance = newBalance;
        this.blockedBalance = blockedBalance;
    }

    public String getAccountname() { return accountname; }
    public void setAccountname(String accountname) { this.accountname = accountname; }

    public long getNewBalance() { return newBalance; }
    public void setNewBalance(long newBalance) { this.newBalance = newBalance; }

    public long getBlockedBalance() { return blockedBalance; }
    public void setBlockedBalance(long blockedBalance) { this.blockedBalance = blockedBalance; }
}
