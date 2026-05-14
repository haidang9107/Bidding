package org.example.model;

import org.example.model.product.Item;
import org.example.model.user.Bidder;

import java.sql.Timestamp;

/**
 * Represents a bid placed by a bidder on an item.
 */
public class Bid {

	// =========================
	// Fields
	// =========================
	private String bidId;

	// Người đấu giá
	private Bidder bidder;

	// Sản phẩm được bid
	private Item item;

	// Giá bid
	private double bidAmount;

	// Thời gian bid
	private Timestamp bidTime;

	/**
	 * Default constructor for Bid.
	 */
	public Bid() {
	}

	/**
	 * Full constructor for Bid.
	 *
	 * @param bidId the unique identifier for the bid
	 * @param bidder the bidder who placed the bid
	 * @param item the item being bid on
	 * @param bidAmount the amount of the bid
	 * @param bidTime the time the bid was placed
	 */
	public Bid(String bidId,
	           Bidder bidder,
	           Item item,
	           double bidAmount,
	           Timestamp bidTime) {

		this.bidId = bidId;
		this.bidder = bidder;
		this.item = item;
		this.bidAmount = bidAmount;
		this.bidTime = bidTime;
	}

	/**
	 * Gets the bid ID.
	 *
	 * @return the bid ID
	 */
	public String getBidId() {
		return bidId;
	}

	/**
	 * Sets the bid ID.
	 *
	 * @param bidId the bid ID to set
	 */
	public void setBidId(String bidId) {
		this.bidId = bidId;
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
	 * Gets the item being bid on.
	 *
	 * @return the item
	 */
	public Item getItem() {
		return item;
	}

	/**
	 * Sets the item being bid on.
	 *
	 * @param item the item to set
	 */
	public void setItem(Item item) {
		this.item = item;
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
