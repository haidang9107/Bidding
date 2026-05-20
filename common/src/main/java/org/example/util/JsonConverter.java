package org.example.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;

/**
 * Utility class for converting Java objects to JSON and vice-versa.
 */
public class JsonConverter {
    // Gson instance is thread-safe, so we can reuse it
    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
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
}
