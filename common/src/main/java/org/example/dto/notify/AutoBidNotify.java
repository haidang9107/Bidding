package org.example.dto.notify;

/**
 * Notification DTO for auto-bid events.
 */
public class AutoBidNotify {
    private int auctionId;
    private String bidderAccountname;

    /**
     * Default constructor for AutoBidNotify.
     */
    public AutoBidNotify() {}

    /**
     * Constructs an AutoBidNotify with specified auction ID and bidder account name.
     * @param auctionId the ID of the auction
     * @param bidderAccountname the account name of the bidder
     */
    public AutoBidNotify(int auctionId, String bidderAccountname) {
        this.auctionId = auctionId;
        this.bidderAccountname = bidderAccountname;
    }

    /**
     * Gets the auction ID.
     * @return the auction ID
     */
    public int getAuctionId() { return auctionId; }

    /**
     * Sets the auction ID.
     * @param auctionId the auction ID to set
     */
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    /**
     * Gets the bidder's account name.
     * @return the bidder's account name
     */
    public String getBidderAccountname() { return bidderAccountname; }

    /**
     * Sets the bidder's account name.
     * @param bidderAccountname the bidder's account name to set
     */
    public void setBidderAccountname(String bidderAccountname) { this.bidderAccountname = bidderAccountname; }
}
