package org.example.payload;

/**
 * Message types for the bidding system.
 */
public enum MessageType {
    /** Authentication: Login request/response */
    LOGIN,
    /** Authentication: Signup request/response */
    SIGNUP,
    /** Authentication: Logout request/response */
    LOGOUT,
    
    /** User Actions: Get user profile */
    GET_PROFILE,
    /** User Actions: Update user profile */
    UPDATE_PROFILE,
    
    /** Auction/Product Management: Get list of products */
    PRODUCT_LIST,
    /** Auction/Product Management: Get product details */
    PRODUCT_DETAIL,
    /** Auction/Product Management: Add a new product */
    PRODUCT_ADD,
    
    /** Bidding Logic: Client sends a bid */
    BID_PLACE,
    /** Bidding Logic: Server broadcasts new highest bid to all clients */
    BID_UPDATE,
    
    /** Real-time Notifications: Server sends remaining time for an auction */
    TIMER_TICK,
    /** Real-time Notifications: Server announces auction started */
    AUCTION_START,
    /** Real-time Notifications: Server announces auction finished (winner) */
    AUCTION_END,
    /** Real-time Notifications: Generic system notification */
    NOTIFICATION,
    
    /** System Status: Success response */
    SUCCESS,
    /** System Status: Error response */
    ERROR
}
