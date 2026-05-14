package org.example.model;

import org.example.model.product.*;
import org.example.model.user.Bidder;

import java.sql.Timestamp;

/**
 * Represents an auction record in the system.
 */
public class Auction {

	// =========================
	// Fields (mapping với bảng auctions)
	// =========================
	private String auctionId;

	// Product được đấu giá
	private Item item;

	// Người đấu giá
	private Bidder bidder;

	// Giá bid
	private double bidAmount;

	// Thời gian bid
	private Timestamp bidTime;

	/**
	 * Default constructor for Auction.
	 */
	public Auction() {
	}

	/**
	 * Full constructor for Auction.
	 *
	 * @param auctionId the unique identifier for the auction
	 * @param item the item being auctioned
	 * @param bidder the bidder who placed the bid
	 * @param bidAmount the amount of the bid
	 * @param bidTime the time the bid was placed
	 */
	public Auction(String auctionId,
	               Item item,
	               Bidder bidder,
	               double bidAmount,
	               Timestamp bidTime) {

		this.auctionId = auctionId;
		this.item = item;
		this.bidder = bidder;
		this.bidAmount = bidAmount;
		this.bidTime = bidTime;
	}

	/**
	 * Gets the auction ID.
	 *
	 * @return the auction ID
	 */
	public String getAuctionId() {
		return auctionId;
	}

	/**
	 * Sets the auction ID.
	 *
	 * @param auctionId the auction ID to set
	 */
	public void setAuctionId(String auctionId) {
		this.auctionId = auctionId;
	}

	/**
	 * Gets the item being auctioned.
	 *
	 * @return the item
	 */
	public Item getItem() {
		return item;
	}

	/**
	 * Sets the item being auctioned.
	 *
	 * @param item the item to set
	 */
	public void setItem(Item item) {
		this.item = item;
	}

	/**
	 * Gets the bidder who placed the bid.
	 *
	 * @return the bidder
	 */
	public Bidder getBidder() {
		return bidder;
	}

	/**
	 * Sets the bidder who placed the bid.
	 *
	 * @param bidder the bidder to set
	 */
	public void setBidder(Bidder bidder) {
		this.bidder = bidder;
	}

	/**
	 * Gets the bid amount.
	 *
	 * @return the bid amount
	 */
	public double getBidAmount() {
		return bidAmount;
	}

	/**
	 * Sets the bid amount.
	 *
	 * @param bidAmount the bid amount to set
	 */
	public void setBidAmount(double bidAmount) {
		this.bidAmount = bidAmount;
	}

	/**
	 * Gets the time the bid was placed.
	 *
	 * @return the bid time
	 */
	public Timestamp getBidTime() {
		return bidTime;
	}

	/**
	 * Sets the time the bid was placed.
	 *
	 * @param bidTime the bid time to set
	 */
	public void setBidTime(Timestamp bidTime) {
		this.bidTime = bidTime;
	}
}
