package org.example.model.product;

import org.example.model.user.Seller;
import java.sql.Timestamp;

/**
 * Represents a generic item in the auction system.
 * This class serves as a base for specific product types.
 */
public class Item {

	// =========================
	// Fields (mapping với bảng products)
	// =========================
	private String productId;

	private String productName;

	private String description;

	private double startingPrice;

	private double stepPrice;

	// seller_id -> object Seller
	private Seller seller;

	// ACTIVE / SOLD / CLOSED
	private String status;

	private Timestamp createdAt;

	/**
	 * Default constructor for Item.
	 */
	public Item() {
	}

	/**
	 * Full constructor for Item.
	 *
	 * @param productId the unique identifier for the product
	 * @param productName the name of the product
	 * @param description a description of the product
	 * @param startingPrice the starting price of the auction
	 * @param stepPrice the minimum increment for bids
	 * @param seller the seller of the product
	 * @param status the current status of the product (e.g., ACTIVE, SOLD, CLOSED)
	 * @param createdAt the timestamp when the product was created
	 */
	public Item(String productId,
	            String productName,
	            String description,
	            double startingPrice,
	            double stepPrice,
	            Seller seller,
	            String status,
	            Timestamp createdAt) {

		this.productId = productId;
		this.productName = productName;
		this.description = description;
		this.startingPrice = startingPrice;
		this.stepPrice = stepPrice;
		this.seller = seller;
		this.status = status;
		this.createdAt = createdAt;
	}

	/**
	 * Gets the product ID.
	 *
	 * @return the product ID
	 */
	public String getProductId() {
		return productId;
	}

	/**
	 * Sets the product ID.
	 *
	 * @param productId the product ID to set
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}

	/**
	 * Gets the product name.
	 *
	 * @return the product name
	 */
	public String getProductName() {
		return productName;
	}

	/**
	 * Sets the product name.
	 *
	 * @param productName the product name to set
	 */
	public void setProductName(String productName) {
		this.productName = productName;
	}

	/**
	 * Gets the product description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the product description.
	 *
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets the starting price of the auction.
	 *
	 * @return the starting price
	 */
	public double getStartingPrice() {
		return startingPrice;
	}

	/**
	 * Sets the starting price of the auction.
	 *
	 * @param startingPrice the starting price to set
	 */
	public void setStartingPrice(double startingPrice) {
		this.startingPrice = startingPrice;
	}

	/**
	 * Gets the minimum increment for bids.
	 *
	 * @return the step price
	 */
	public double getStepPrice() {
		return stepPrice;
	}

	/**
	 * Sets the minimum increment for bids.
	 *
	 * @param stepPrice the step price to set
	 */
	public void setStepPrice(double stepPrice) {
		this.stepPrice = stepPrice;
	}

	/**
	 * Gets the seller of the product.
	 *
	 * @return the seller
	 */
	public Seller getSeller() {
		return seller;
	}

	/**
	 * Sets the seller of the product.
	 *
	 * @param seller the seller to set
	 */
	public void setSeller(Seller seller) {
		this.seller = seller;
	}

	/**
	 * Gets the current status of the product.
	 *
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Sets the current status of the product.
	 *
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Gets the timestamp when the product was created.
	 *
	 * @return the creation timestamp
	 */
	public Timestamp getCreatedAt() {
		return createdAt;
	}

	/**
	 * Sets the timestamp when the product was created.
	 *
	 * @param createdAt the creation timestamp to set
	 */
	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}
}
