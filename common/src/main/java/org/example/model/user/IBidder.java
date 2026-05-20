package org.example.model.user;

/**
 * Interface defining the behaviors of a bidder.
 */
public interface IBidder {
    void placeBid(int productId, long amount);
    // Có thể thêm các method như viewMyBids(),...
}
