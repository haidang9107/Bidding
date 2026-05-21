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
        DEFAULTS.put("BID_DB_URL", "jdbc:mysql://localhost:3306/bidding_db?createDatabaseIfNotExist=true");
        DEFAULTS.put("BID_DB_DRIVER", "com.mysql.cj.jdbc.Driver");

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
