package org.example.model.product;

import com.google.gson.*;
import java.lang.reflect.Type;

/**
 * Factory for creating specific Product types based on category.
 * Implements the Factory Method pattern by acting as a GSON JsonDeserializer.
 */
public class ProductFactory implements JsonDeserializer<Product> {

    @Override
    public Product deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        
        if (!jsonObject.has("category")) {
            throw new JsonParseException("Missing 'category' field for Product deserialization");
        }
        
        String category = jsonObject.get("category").getAsString();
        
        return switch (category) {
            case "ELECTRONICS" -> context.deserialize(jsonObject, Electronics.class);
            case "ART" -> context.deserialize(jsonObject, Art.class);
            case "VEHICLE" -> context.deserialize(jsonObject, Vehicle.class);
            case "OTHER" -> context.deserialize(jsonObject, OtherItem.class);
            default -> throw new JsonParseException("Unknown product category: " + category);
        };
    }
}
