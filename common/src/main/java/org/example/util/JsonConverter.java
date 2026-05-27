package org.example.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Utility class for converting Java objects to JSON and vice-versa.
 */
public class JsonConverter {

    /**
     * A global adapter factory that automatically flattens any "metadata" object
     * found in the incoming JSON payload into the root level before deserialization.
     */
    private static class MetadataFlatteningAdapterFactory implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    delegate.write(out, value);
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    JsonElement jsonElement = elementAdapter.read(in);
                    if (jsonElement.isJsonObject()) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        if (jsonObject.has("metadata") && jsonObject.get("metadata").isJsonObject()) {
                            JsonObject metadata = jsonObject.getAsJsonObject("metadata");
                            for (Map.Entry<String, JsonElement> entry : metadata.entrySet()) {
                                jsonObject.add(entry.getKey(), entry.getValue());
                            }
                            jsonObject.remove("metadata");
                        }
                        return delegate.fromJsonTree(jsonObject);
                    }
                    return delegate.fromJsonTree(jsonElement);
                }
            };
        }
    }

    // Gson instance is thread-safe, so we can reuse it. Order matters: Flattener runs first.
    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapterFactory(new MetadataFlatteningAdapterFactory())
            .create();

    /**
     * Converts an object to its JSON string representation.
     *
     * @param object the object to convert
     * @return the JSON string
     */
    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    /**
     * Converts a JSON string back to a Java object.
     *
     * @param json the JSON string
     * @param classOfT the class of the object
     * @param <T> the type of the object
     * @return the Java object
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    /**
     * Converts a JSON string to a Java object using a Type (useful for Generics like List&lt;User&gt;).
     *
     * @param json the JSON string
     * @param typeOfT the type of the object
     * @param <T> the type of the object
     * @return the Java object
     */
    public static <T> T fromJson(String json, Type typeOfT) {
        return gson.fromJson(json, typeOfT);
    }

    /**
     * Efficiently converts an object (usually a Map from GSON) to a specific class.
     * 
     * @param from the source object
     * @param to the target class
     * @param <T> the target type
     * @return the converted object
     */
    public static <T> T convert(Object from, Class<T> to) {
        if (from == null) return null;
        if (to.isInstance(from)) return to.cast(from);
        return gson.fromJson(gson.toJsonTree(from), to);
    }

    /**
     * Masks sensitive fields like 'password' in JSON strings for safe logging.
     */
    public static String maskSensitiveData(String json) {
        if (json == null) return null;
        return json.replaceAll("(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1***$2");
    }
}
