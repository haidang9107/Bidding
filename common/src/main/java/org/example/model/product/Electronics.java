package org.example.model.product;

import org.example.model.user.Seller;

import java.sql.Timestamp;

public class Electronics extends Item {

	// =========================
	// Thuộc tính riêng
	// =========================
	private String brand;

	private int warrantyMonths;

	// =========================
	// Constructor rỗng
	// =========================
	public Electronics() {
		super();
	}

	// =========================
	// Constructor đầy đủ cả field riêng
	// =========================
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

	public int getWarrantyMonths() {
		return warrantyMonths;
	}

	public void setWarrantyMonths(int warrantyMonths) {
		this.warrantyMonths = warrantyMonths;
	}
}