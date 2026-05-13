package org.example.model;

import org.example.model.product.Item;
import org.example.model.user.Bidder;

import java.sql.Timestamp;

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

	// =========================
	// Constructor rỗng
	// =========================
	public Bid() {
	}

	// =========================
	// Constructor đầy đủ
	// =========================
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

	// =========================
	// Getter & Setter
	// =========================

	public String getBidId() {
		return bidId;
	}

	public void setBidId(String bidId) {
		this.bidId = bidId;
	}

	// -------------------------

	public Bidder getBidder() {
		return bidder;
	}

	public void setBidder(Bidder bidder) {
		this.bidder = bidder;
	}

	// -------------------------

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	// -------------------------

	public double getBidAmount() {
		return bidAmount;
	}

	public void setBidAmount(double bidAmount) {
		this.bidAmount = bidAmount;
	}

	// -------------------------

	public Timestamp getBidTime() {
		return bidTime;
	}

	public void setBidTime(Timestamp bidTime) {
		this.bidTime = bidTime;
	}
}