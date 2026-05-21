package org.example.server.repository;

import org.example.util.FileLogger;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class SchemaRunner {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Running schema initialization...");
            runScript(stmt, "/MySQL/1schema.sql");
            runScript(stmt, "/MySQL/2data.sql");
            System.out.println("Schema initialization completed successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void runScript(Statement stmt, String path) throws Exception {
        InputStream is = SchemaRunner.class.getResourceAsStream(path);
        if (is == null) {
            throw new RuntimeException("Script not found: " + path);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String content = reader.lines().collect(Collectors.joining("\n"));
            String[] commands = content.split(";");
            for (String cmd : commands) {
                String trimmed = cmd.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        stmt.execute(trimmed);
                    } catch (Exception e) {
                        System.err.println("Warning: Failed to execute: " + trimmed + " - " + e.getMessage());
                    }
                }
            }
        }
    }
}
