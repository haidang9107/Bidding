package org.example.model.product;

import org.example.model.enums.ItemCategory;

/**
 * Represents a vehicle product.
 */
public class Vehicle extends Product {

    private String brand;
    private String model;
    private int manufactureYear;

    /**
     * Default constructor.
     */
    public Vehicle() {
        super();
        setCategory(ItemCategory.VEHICLE);
    }

    /**
     * Constructs a Vehicle product with all fields.
     * @param productId        The unique product ID.
     * @param name             The product name.
     * @param description      The product description.
     * @param imageUrl         The image URL.
     * @param ownerAccountname The current owner of the product.
     * @param brand            The brand.
     * @param model            The model.
     * @param manufactureYear  The year of manufacture.
     */
    public Vehicle(int productId, String name, String description, String imageUrl,
                   String ownerAccountname, String brand, String model, int manufactureYear) {
        super(productId, name, description, imageUrl, ItemCategory.VEHICLE, ownerAccountname);
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
