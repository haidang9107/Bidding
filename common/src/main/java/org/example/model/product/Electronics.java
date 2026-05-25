package org.example.model.product;

import org.example.model.enums.ItemCategory;

/**
 * Represents an electronic product.
 */
public class Electronics extends Product {

    private String brand;
    private int warrantyMonths;

    /**
     * Default constructor.
     */
    public Electronics() {
        super();
        setCategory(ItemCategory.ELECTRONICS);
    }

    /**
     * Constructs an Electronics product with all fields.
     * @param productId        The unique product ID.
     * @param name             The product name.
     * @param description      The product description.
     * @param imageUrl         The image URL.
     * @param ownerAccountname The current owner of the product.
     * @param brand            The brand.
     * @param warrantyMonths   The warranty duration in months.
     */
    public Electronics(int productId, String name, String description, String imageUrl,
                       String ownerAccountname, String brand, int warrantyMonths) {
        super(productId, name, description, imageUrl, ItemCategory.ELECTRONICS, ownerAccountname);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
}
