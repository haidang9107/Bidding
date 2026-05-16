package org.example.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Enhanced Utility class for logging, splitting Audit and Error logs into separate files.
 */
public class FileLogger {
    private static final String LOG_DIR = "common/logs/";
    private static final String AUDIT_LOG = LOG_DIR + "audit.log";
    private static final String ERROR_LOG = LOG_DIR + "error.log";
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Logs business events and connections (Audit trail).
     * @param message the message to log
     */
    public static synchronized void info(String message) {
        log(AUDIT_LOG, "INFO", message, null);
    }

    /**
     * Logs critical system errors.
     * @param message the error message
     */
    public static synchronized void error(String message) {
        log(ERROR_LOG, "ERROR", message, null);
    }

    /**
     * Logs critical system errors with stack trace.
     * @param message the error message
     * @param throwable the exception
     */
    public static synchronized void error(String message, Throwable throwable) {
        log(ERROR_LOG, "ERROR", message, throwable);
    }

    /**
     * Core logging method with file separation logic.
     */
    private static void log(String fileName, String level, String message, Throwable throwable) {
        try (FileWriter fw = new FileWriter(fileName, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            String timestamp = LocalDateTime.now().format(formatter);
            out.printf("[%s] [%s] %s%n", timestamp, level, message);
            
            if (throwable != null) {
                throwable.printStackTrace(out);
            }
            
            // Also print to console for real-time visibility during development
            System.out.printf("[%s] [%s] %s%n", timestamp, level, message);
            
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + fileName);
        }
    }
}
