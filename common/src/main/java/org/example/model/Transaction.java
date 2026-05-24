package org.example.model;

import org.example.model.enums.TransactionType;
import java.sql.Timestamp;

/**
 * Represents a financial transaction or audit record in the system.
 */
public class Transaction {
    private int transactionId;
    private String senderAccountname;
    private String receiverAccountname;
    private TransactionType type;
    private Integer productId;
    private long amount;
    private Integer referenceId; // e.g., auctionId
    private String description;
    private Timestamp createdAt;

    public Transaction() {}

    public Transaction(int transactionId, String senderAccountname, String receiverAccountname,
                       TransactionType type, Integer productId, long amount,
                       Integer referenceId, String description, Timestamp createdAt) {
        this.transactionId = transactionId;
        this.senderAccountname = senderAccountname;
        this.receiverAccountname = receiverAccountname;
        this.type = type;
        this.productId = productId;
        this.amount = amount;
        this.referenceId = referenceId;
        this.description = description;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public String getSenderAccountname() { return senderAccountname; }
    public void setSenderAccountname(String senderAccountname) { this.senderAccountname = senderAccountname; }

    public String getReceiverAccountname() { return receiverAccountname; }
    public void setReceiverAccountname(String receiverAccountname) { this.receiverAccountname = receiverAccountname; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public Integer getReferenceId() { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
