package org.example.util;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized configuration utility for the application.
 * Holds default values and overrides them with environment variables if present.
 */
public class Config {
    private static final Dotenv dotenv;
    private static final Map<String, String> DEFAULTS = new HashMap<>();

    static {
        // Define all system defaults here
        DEFAULTS.put("SERVER_PORT", "8888");
        DEFAULTS.put("NOTIFY_PORT", "8889");
        DEFAULTS.put("SERVER_HOST", "localhost");
        
        DEFAULTS.put("DB_URL", "jdbc:mysql://localhost:3306/MySQL-DB?createDatabaseIfNotExist=true");
        DEFAULTS.put("DB_USER", "HaiDang91");
        DEFAULTS.put("DB_PASSWORD", "9107");

        dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
    }

    /**
     * Retrieves an environment variable or its default value.
     * @param key The key of the configuration.
     * @return The value from .env or the predefined default.
     */
    public static String get(String key) {
        String value = dotenv.get(key);
        return (value != null) ? value : DEFAULTS.getOrDefault(key, null);
    }

    /**
     * Retrieves a configuration value as an integer.
     * @param key The key of the configuration.
     * @return The value as an integer.
     */
    public static int getInt(String key) {
        String value = get(key);
        try {
            return (value != null) ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
