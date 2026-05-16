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
        DEFAULTS.put("NOTIFY_PORT", "8889");
        DEFAULTS.put("SERVER_HOST", "localhost");
        DEFAULTS.put("DB_URL", "jdbc:mysql://localhost:3306/MySQL-DB?createDatabaseIfNotExist=true");
        DEFAULTS.put("DB_USER", "HaiDang91");
        DEFAULTS.put("DB_PASSWORD", "9107");

        // Logic to find the .env at project root
        String userDir = System.getProperty("user.dir");
        File envFile = new File(userDir, ".env");
        
        // If not found in current dir, check parent (useful if running from within a module folder)
        if (!envFile.exists()) {
            envFile = new File(new File(userDir).getParent(), ".env");
        }

        dotenv = Dotenv.configure()
                .directory(envFile.getParent())
                .ignoreIfMissing()
                .load();
    }

    public static String get(String key) {
        String value = dotenv.get(key);
        return (value != null) ? value : DEFAULTS.getOrDefault(key, null);
    }

    public static int getInt(String key) {
        String value = get(key);
        try {
            return (value != null) ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
