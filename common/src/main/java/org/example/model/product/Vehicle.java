package org.example.model.product;

import org.example.model.user.Seller;

import java.sql.Timestamp;

/**
 * Represents a vehicle item in the auction system.
 */
public class Vehicle extends Item {

	// =========================
	// Thuộc tính riêng
	// =========================
	private String brand;

	private String model;

	private int manufactureYear;

	/**
	 * Default constructor for Vehicle.
	 */
	public Vehicle() {
		super();
	}

	/**
	 * Full constructor for Vehicle.
	 *
	 * @param productId the unique identifier for the product
	 * @param productName the name of the product
	 * @param description a description of the product
	 * @param startingPrice the starting price of the auction
	 * @param stepPrice the minimum increment for bids
	 * @param seller the seller of the product
	 * @param status the current status of the product
	 * @param createdAt the timestamp when the product was created
	 * @param brand the brand of the vehicle
	 * @param model the model of the vehicle
	 * @param manufactureYear the year the vehicle was manufactured
	 */
	public Vehicle(String productId,
	               String productName,
	               String description,
	               double startingPrice,
	               double stepPrice,
	               Seller seller,
	               String status,
	               Timestamp createdAt,
	               String brand,
	               String model,
	               int manufactureYear) {

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
		this.model = model;
		this.manufactureYear = manufactureYear;
	}

	/**
	 * Gets the brand of the vehicle.
	 *
	 * @return the brand
	 */
	public String getBrand() {
		return brand;
	}

	/**
	 * Sets the brand of the vehicle.
	 *
	 * @param brand the brand to set
	 */
	public void setBrand(String brand) {
		this.brand = brand;
	}

	/**
	 * Gets the model of the vehicle.
	 *
	 * @return the model
	 */
	public String getModel() {
		return model;
	}

	/**
	 * Sets the model of the vehicle.
	 *
	 * @param model the model to set
	 */
	public void setModel(String model) {
		this.model = model;
	}

	/**
	 * Gets the manufacture year of the vehicle.
	 *
	 * @return the manufacture year
	 */
	public int getManufactureYear() {
		return manufactureYear;
	}

	/**
	 * Sets the manufacture year of the vehicle.
	 *
	 * @param manufactureYear the manufacture year to set
	 */
	public void setManufactureYear(int manufactureYear) {
		this.manufactureYear = manufactureYear;
	}
}
