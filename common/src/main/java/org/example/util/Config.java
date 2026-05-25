package org.example.util;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized configuration utility. 
 * Locates the .env file at the project root regardless of the current working directory.
 */
public class Config {
    private static final Dotenv dotenv;
    private static final Map<String, String> DEFAULTS = new HashMap<>();

    static {
        // Define system defaults
        DEFAULTS.put("SERVER_PORT", "8888");
        DEFAULTS.put("SERVER_HOST", "localhost");
        DEFAULTS.put("DB_HOST", "localhost");
        DEFAULTS.put("DB_PORT", "3306");
        DEFAULTS.put("DB_NAME", "bidding_db");
        DEFAULTS.put("DB_URL", "jdbc:mysql://localhost:3306/bidding_db?createDatabaseIfNotExist=true");
        DEFAULTS.put("DB_DRIVER", "com.mysql.cj.jdbc.Driver");
        DEFAULTS.put("DB_USER", "root");
        DEFAULTS.put("DB_PASSWORD", "root");
        
        // Advanced performance & logic defaults
        DEFAULTS.put("DB_MAX_POOL_SIZE", "6");
        DEFAULTS.put("SERVER_WORKER_THREADS", "6");
        DEFAULTS.put("SERVER_QUEUE_CAPACITY", "128");
        DEFAULTS.put("ANTI_SNIP_WINDOW_MS", "60000"); // 1 minute
        DEFAULTS.put("ANTI_SNIP_EXTENSION_MS", "300000"); // 5 minutes

        // Logic to find the .env at project root
        String userDir = System.getProperty("user.dir");
        File envFile = findEnvFile(new File(userDir));

        dotenv = Dotenv.configure()
                .directory(envFile != null ? envFile.getParent() : userDir)
                .ignoreIfMissing()
                .load();
    }

    private static File findEnvFile(File currentDir) {
        if (currentDir == null) return null;
        File env = new File(currentDir, ".env");
        if (env.exists()) return env;
        return findEnvFile(currentDir.getParentFile());
    }

    /**
     * Retrieves a configuration value as a String.
     * @param key The configuration key.
     * @return The configuration value, or a default value if not found.
     */
    public static String get(String key) {
        String value = dotenv.get(key);
        return (value != null && !value.isBlank()) ? value : DEFAULTS.getOrDefault(key, null);
    }

    /**
     * Retrieves a configuration value as an integer.
     * @param key The configuration key.
     * @return The configuration value as an integer, or a default value if not found or invalid.
     */
    public static int getInt(String key) {
        String envValue = dotenv.get(key);
        if (envValue != null && !envValue.isBlank()) {
            try {
                return Integer.parseInt(envValue.trim());
            } catch (NumberFormatException ignored) {
                // Ignore parse error, fallback to default
            }
        }
        
        String defValue = DEFAULTS.get(key);
        if (defValue != null) {
            try {
                return Integer.parseInt(defValue.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    /**
     * Retrieves a configuration value as a boolean.
     * @param key The configuration key.
     * @return True if the value is "true" (case-insensitive), false otherwise.
     */
    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }
}
