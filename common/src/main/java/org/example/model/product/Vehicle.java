package org.example.model.product;

import org.example.model.user.Seller;

import java.sql.Timestamp;

public class Vehicle extends Item {

	// =========================
	// Thuộc tính riêng
	// =========================
	private String brand;

	private String model;

	private int manufactureYear;

	// =========================
	// Constructor rỗng
	// =========================
	public Vehicle() {
		super();
	}

	// =========================
	// Constructor đầy đủ
	// =========================
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

	// =========================
	// Getter & Setter
	// =========================

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	// -------------------------

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	// -------------------------

	public int getManufactureYear() {
		return manufactureYear;
	}

	public void setManufactureYear(int manufactureYear) {
		this.manufactureYear = manufactureYear;
	}
}