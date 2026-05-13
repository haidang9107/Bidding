package org.example.model.product;

import org.example.model.user.Seller;

import java.sql.Timestamp;

public class Art extends Item {

	// =========================
	// Thuộc tính riêng
	// =========================
	private String artist;

	private String artType;

	// =========================
	// Constructor rỗng
	// =========================
	public Art() {
		super();
	}

	// =========================
	// Constructor
	// =========================
	public Art(String productId,
	           String productName,
	           String description,
	           double startingPrice,
	           double stepPrice,
	           Seller seller,
	           String status,
	           Timestamp createdAt,
	           String artist,
	           String artType) {

		super(
				productId,
				productName,
				description,
				startingPrice,
				stepPrice,
				seller,
				status,
				createdAt
		);

		this.artist = artist;
		this.artType = artType;
	}

	// =========================
	// Getter & Setter
	// =========================

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	// -------------------------

	public String getArtType() {
		return artType;
	}

	public void setArtType(String artType) {
		this.artType = artType;
	}
}