package org.example.dto.request;

/**
 * Data Transfer Object for transferring funds between users.
 */
public class TransferRequest {
    private String toAccountname;
    private long amount;

    /**
     * Default constructor for TransferRequest.
     */
    public TransferRequest() {}

    /**
     * Constructs a TransferRequest with recipient and amount.
     * @param toAccountname the account name of the recipient
     * @param amount the amount to transfer
     */
    public TransferRequest(String toAccountname, long amount) {
        this.toAccountname = toAccountname;
        this.amount = amount;
    }

    /**
     * Gets the recipient's account name.
     * @return the recipient's account name
     */
    public String getToAccountname() { return toAccountname; }

    /**
     * Sets the recipient's account name.
     * @param toAccountname the recipient's account name to set
     */
    public void setToAccountname(String toAccountname) {
        this.toAccountname = toAccountname;
    }

    /**
     * Gets the transfer amount.
     * @return the amount
     */
    public long getAmount() { return amount; }

    /**
     * Sets the transfer amount.
     * @param amount the amount to set
     */
    public void setAmount(long amount) {
        this.amount = amount;
    }
}