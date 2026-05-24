package org.example.model.product;

import org.example.model.enums.ItemCategory;
import org.example.util.JsonConverter;

/**
 * Factory for creating specific Item types based on category.
 * Implements the Factory Method pattern as per project requirements.
 */
public class ItemFactory {

    /**
     * Creates an Item instance based on the provided category and data.
     *
     * @param category the category of the item
     * @param dataJson the JSON representation of the item data
     * @return a specific subclass of Item
     */
    public static Item createItem(ItemCategory category, String dataJson) {
        return switch (category) {
            case ELECTRONICS -> JsonConverter.fromJson(dataJson, Electronics.class);
            case ART -> JsonConverter.fromJson(dataJson, Art.class);
            case VEHICLE -> JsonConverter.fromJson(dataJson, Vehicle.class);
            case OTHER -> JsonConverter.fromJson(dataJson, OtherItem.class);
        };
    }
}
