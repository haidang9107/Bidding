package org.example.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for logging messages to a file.
 */
public class FileLogger {
    private static final String LOG_FILE = "common/logs/app.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Logs an info message.
     *
     * @param message the message to log
     */
    public static synchronized void info(String message) {
        log("INFO", message, null);
    }

    /**
     * Logs an error message.
     *
     * @param message the message to log
     */
    public static synchronized void error(String message) {
        log("ERROR", message, null);
    }

    /**
     * Logs an error message with a stack trace.
     *
     * @param message the message to log
     * @param throwable the exception to log
     */
    public static synchronized void error(String message, Throwable throwable) {
        log("ERROR", message, throwable);
    }

    /**
     * Internal method to write logs to the file.
     *
     * @param level the log level (e.g., INFO, ERROR)
     * @param message the log message
     * @param throwable optional exception
     */
    private static void log(String level, String message, Throwable throwable) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            String timestamp = LocalDateTime.now().format(formatter);
            out.printf("[%s] [%s] %s%n", timestamp, level, message);
            
            if (throwable != null) {
                throwable.printStackTrace(out);
            }
        } catch (IOException e) {
            // Fallback if file logging fails
        }
    }
}
