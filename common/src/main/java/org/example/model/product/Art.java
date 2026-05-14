package org.example.model.product;

import org.example.model.user.Seller;

import java.sql.Timestamp;

/**
 * Represents an artwork item in the auction system.
 */
public class Art extends Item {

	// =========================
	// Thuộc tính riêng
	// =========================
	private String artist;

	private String artType;

	/**
	 * Default constructor for Art.
	 */
	public Art() {
		super();
	}

	/**
	 * Full constructor for Art.
	 *
	 * @param productId the unique identifier for the product
	 * @param productName the name of the product
	 * @param description a description of the product
	 * @param startingPrice the starting price of the auction
	 * @param stepPrice the minimum increment for bids
	 * @param seller the seller of the product
	 * @param status the current status of the product
	 * @param createdAt the timestamp when the product was created
	 * @param artist the artist who created the artwork
	 * @param artType the type of art (e.g., Painting, Sculpture)
	 */
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

	/**
	 * Gets the artist of the artwork.
	 *
	 * @return the artist
	 */
	public String getArtist() {
		return artist;
	}

	/**
	 * Sets the artist of the artwork.
	 *
	 * @param artist the artist to set
	 */
	public void setArtist(String artist) {
		this.artist = artist;
	}

	/**
	 * Gets the type of art.
	 *
	 * @return the art type
	 */
	public String getArtType() {
		return artType;
	}

	/**
	 * Sets the type of art.
	 *
	 * @param artType the art type to set
	 */
	public void setArtType(String artType) {
		this.artType = artType;
	}
}
