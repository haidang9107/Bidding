package org.example.model.product;

import org.example.model.enums.ItemCategory;
import org.example.util.JsonConverter;

/**
 * Factory for creating specific Product types based on category.
 * Implements the Factory Method pattern as per project requirements.
 */
public class ItemFactory {

    /**
     * Creates a Product instance based on the provided category and data.
     *
     * @param category the category of the product
     * @param dataJson the JSON representation of the product data
     * @return a specific subclass of Product
     */
    public static Product createProduct(ItemCategory category, String dataJson) {
        return switch (category) {
            case ELECTRONICS -> JsonConverter.fromJson(dataJson, Electronics.class);
            case ART -> JsonConverter.fromJson(dataJson, Art.class);
            case VEHICLE -> JsonConverter.fromJson(dataJson, Vehicle.class);
            case OTHER -> JsonConverter.fromJson(dataJson, OtherItem.class);
        };
    }
}
