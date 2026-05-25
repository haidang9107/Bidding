package org.example.model.product;

import org.example.model.enums.ItemCategory;

/**
 * Represents an artwork product.
 */
public class Art extends Product {

    private String artist;
    private String artType;

    /**
     * Default constructor.
     */
    public Art() {
        super();
        setCategory(ItemCategory.ART);
    }

    /**
     * Constructs an Art product with all fields.
     * @param productId        The unique product ID.
     * @param name             The product name.
     * @param description      The product description.
     * @param imageUrl         The image URL.
     * @param ownerAccountname The current owner of the product.
     * @param artist           The artist's name.
     * @param artType          The art type.
     */
    public Art(int productId, String name, String description, String imageUrl,
               String ownerAccountname, String artist, String artType) {
        super(productId, name, description, imageUrl, ItemCategory.ART, ownerAccountname);
        this.artist = artist;
        this.artType = artType;
    }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getArtType() { return artType; }
    public void setArtType(String artType) { this.artType = artType; }
}
