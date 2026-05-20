package org.example.dto;

public class TransferRequest {
    private int toUserId;
    private long amount;

    public TransferRequest() {}
    public TransferRequest(int toUserId, long amount) {
        this.toUserId = toUserId;
        this.amount = amount;
    }

    public int getToUserId() { return toUserId; }
    public long getAmount() { return amount; }
}
