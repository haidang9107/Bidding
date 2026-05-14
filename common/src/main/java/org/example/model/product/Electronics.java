package org.example.model.product;

import org.example.model.user.Seller;

import java.sql.Timestamp;

/**
 * Represents an electronics item in the auction system.
 */
public class Electronics extends Item {

	// =========================
	// Thuộc tính riêng
	// =========================
	private String brand;

	private int warrantyMonths;

	/**
	 * Default constructor for Electronics.
	 */
	public Electronics() {
		super();
	}

	/**
	 * Full constructor for Electronics.
	 *
	 * @param productId the unique identifier for the product
	 * @param productName the name of the product
	 * @param description a description of the product
	 * @param startingPrice the starting price of the auction
	 * @param stepPrice the minimum increment for bids
	 * @param seller the seller of the product
	 * @param status the current status of the product
	 * @param createdAt the timestamp when the product was created
	 * @param brand the brand of the electronics
	 * @param warrantyMonths the warranty period in months
	 */
	public Electronics(String productId,
	                   String productName,
	                   String description,
	                   double startingPrice,
	                   double stepPrice,
	                   Seller seller,
	                   String status,
	                   Timestamp createdAt,
	                   String brand,
	                   int warrantyMonths) {

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

		this.brand = brand;
		this.warrantyMonths = warrantyMonths;
	}

	/**
	 * Gets the brand of the electronics.
	 *
	 * @return the brand
	 */
	public String getBrand() {
		return brand;
	}

	/**
	 * Sets the brand of the electronics.
	 *
	 * @param brand the brand to set
	 */
	public void setBrand(String brand) {
		this.brand = brand;
	}

	/**
	 * Gets the warranty period in months.
	 *
	 * @return the warranty months
	 */
	public int getWarrantyMonths() {
		return warrantyMonths;
	}

	/**
	 * Sets the warranty period in months.
	 *
	 * @param warrantyMonths the warranty months to set
	 */
	public void setWarrantyMonths(int warrantyMonths) {
		this.warrantyMonths = warrantyMonths;
	}
}
