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

    public Vehicle(int productId, String name, String description, String imageUrl, 
                   long startingPrice, long currentPrice, long stepPrice, 
                   String sellerAccountname, String winnerAccountname, 
                   AuctionStatus status, Timestamp startTime, Timestamp endTime, 
                   int version, String brand, String model, int manufactureYear) {
        super(productId, name, description, imageUrl, startingPrice, currentPrice, stepPrice, 
              sellerAccountname, winnerAccountname, ItemCategory.VEHICLE, status, 
              startTime, endTime, version);
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
