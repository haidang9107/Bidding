package org.example.payload;

public enum MessageType {
    // Auth
    LOGIN,
    LOGOUT,
    
    // Auction Logic
    BID_PLACE,      // Client sends a bid
    BID_UPDATE,     // Server broadcasts new highest bid
    PRODUCT_LIST,   // Server sends list of products
    
    // Status
    TIMER_TICK,     // Server sends remaining time
    AUCTION_END,    // Server announces winner
    
    // System
    ERROR,
    SUCCESS
}
