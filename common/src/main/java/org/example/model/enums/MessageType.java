package org.example.model.enums;

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
    /** User Actions: Update user avatar */
    USER_UPDATE_AVATAR,
    
    /** Admin Actions: Get all users */
    ADMIN_GET_ALL_USERS,
    /** Admin Actions: Ban/Unban a user */
    ADMIN_BAN_USER,
    /** Admin Actions: Cancel an ongoing auction */
    ADMIN_CANCEL_AUCTION,
    
    /** Auction/Product Management: Get list of products */
    PRODUCT_LIST,
    /** Auction/Product Management: Get product details */
    PRODUCT_DETAIL,
    /** Auction/Product Management: Add a new product */
    PRODUCT_ADD,
    
    /** Bidding Logic: Client sends a bid */
    BID_PLACE,
    /** Bidding Logic: Client configures automatic bidding */
    AUTO_BID_SET,
    /** Bidding Logic: Client disables automatic bidding */
    AUTO_BID_CANCEL,
    /** Bidding Logic: Client gets bid history */
    BID_HISTORY,
    /** Bidding Logic: Client joins a realtime auction room */
    JOIN_AUCTION_ROOM,
    /** Bidding Logic: Client leaves a realtime auction room */
    LEAVE_AUCTION_ROOM,
    /** Bidding Logic: Server broadcasts new highest bid to all clients */
    BID_UPDATE,
    
    /** Finance Actions: Deposit money */
    DEPOSIT,
    /** Finance Actions: Withdraw money */
    WITHDRAW,
    /** Finance Actions: Transfer money between users */
    TRANSFER,
    /** Finance Actions: Get transaction history */
    TRANSACTION_HISTORY,
    
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
    ERROR,
    /** System Status: Heartbeat check */
    PING,
    /** System Status: Heartbeat response */
    PONG
}
