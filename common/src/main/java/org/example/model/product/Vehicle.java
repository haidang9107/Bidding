package org.example.model.product;

import org.example.model.enums.ItemCategory;
import org.example.model.enums.AuctionStatus;
import java.sql.Timestamp;

/**
 * Represents a vehicle.
 */
public class Vehicle extends Item {

    private String brand;
    private String model;
    private int manufactureYear;

    public Vehicle() {
        super();
        this.setCategory(ItemCategory.VEHICLE);
    }

    public Vehicle(int productId, String productName, String description, long startingPrice, 
                   long currentPrice, long stepPrice, int sellerId, Integer winnerId, 
                   AuctionStatus status, Timestamp startTime, Timestamp endTime, 
                   int version, Timestamp createdAt, String brand, String model, int manufactureYear) {
        super(productId, productName, description, startingPrice, currentPrice, stepPrice, 
              sellerId, winnerId, ItemCategory.VEHICLE, status, startTime, endTime, version, createdAt);
        this.brand = brand;
        this.model = model;
        this.manufactureYear = manufactureYear;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getManufactureYear() { return manufactureYear; }
    public void setManufactureYear(int manufactureYear) { this.manufactureYear = manufactureYear; }
}
