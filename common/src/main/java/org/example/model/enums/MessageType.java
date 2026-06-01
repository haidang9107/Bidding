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
    /** User Actions: Update user password */
    UPDATE_PASSWORD,
    /** User Actions: Update user avatar */
    USER_UPDATE_AVATAR,
    
    /** Admin Actions: Get all users */
    ADMIN_GET_ALL_USERS,
    /** Admin Actions: Ban/Unban a user */
    ADMIN_BAN_USER,
    /** Admin Actions: Cancel an ongoing auction */
    ADMIN_CANCEL_AUCTION,
    /** Admin Actions: Get system-wide statistics */
    ADMIN_GET_STATS,
    
    /** Auction/Product Management: Get list of products */
    PRODUCT_LIST,
    /** Auction/Product Management: Search and filter products/auctions */
    PRODUCT_SEARCH,
    /** Auction/Product Management: Get product details */
    PRODUCT_DETAIL,
    /** Auction/Product Management: Add a new product (legacy: also opens auction) */
    PRODUCT_ADD,
    /** Auction/Product Management: Create a product in the seller's inventory only (no auction) */
    PRODUCT_CREATE,
    /** Auction/Product Management: Update an existing inventory product */
    PRODUCT_UPDATE,
    /** Auction/Product Management: Withdraw a product from inventory (soft delete) */
    PRODUCT_WITHDRAW,
    /** Auction/Product Management: Get the current user's owned products (inventory) */
    MY_PRODUCT_LIST,
    /** Auction/Product Management: Open an auction for an existing inventory product */
    AUCTION_OPEN,
    
    /** Watchlist Actions */
    WATCHLIST_ADD,
    WATCHLIST_REMOVE,
    WATCHLIST_GET,
    
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
    /** Bidding Logic: Server notifies a specific user about balance change */
    BALANCE_UPDATE,
    
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
