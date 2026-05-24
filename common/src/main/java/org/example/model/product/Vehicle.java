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

    /**
     * Default constructor for Vehicle.
     */
    public Vehicle() {
        super();
        this.setCategory(ItemCategory.VEHICLE);
    }

    /**
     * Constructs a Vehicle item with all fields.
     * @param productId The unique product ID.
     * @param name The item name.
     * @param description The item description.
     * @param imageUrl The URL for the item's image.
     * @param startingPrice The initial price.
     * @param currentPrice The current highest bid.
     * @param stepPrice The minimum bid increment.
     * @param sellerAccountname The account name of the seller.
     * @param winnerAccountname The account name of the current winner.
     * @param status The auction status.
     * @param startTime The auction start time.
     * @param endTime The auction end time.
     * @param version The version for optimistic locking.
     * @param brand The brand of the vehicle.
     * @param model The model of the vehicle.
     * @param manufactureYear The year of manufacture.
     */
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
