package org.example.dto.request;

public class TransferRequest {
    private String toAccountname;
    private long amount;

    public TransferRequest() {}
    public TransferRequest(String toAccountname, long amount) {
        this.toAccountname = toAccountname;
        this.amount = amount;
    }

    public String getToAccountname() { return toAccountname; }
    public long getAmount() { return amount; }
}
