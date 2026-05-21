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
    private static final String LOG_DIR;
    private static final String AUDIT_LOG;
    private static final String ERROR_LOG;
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        // Find project root by looking for .env
        String userDir = System.getProperty("user.dir");
        File projectRoot = findProjectRoot(new File(userDir));
        String rootPath = projectRoot != null ? projectRoot.getAbsolutePath() : userDir;
        
        LOG_DIR = rootPath + File.separator + "common" + File.separator + "logs" + File.separator;
        AUDIT_LOG = LOG_DIR + "audit.log";
        ERROR_LOG = LOG_DIR + "error.log";

        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private static File findProjectRoot(File currentDir) {
        if (currentDir == null) return null;
        File env = new File(currentDir, ".env");
        if (env.exists()) return currentDir;
        return findProjectRoot(currentDir.getParentFile());
    }

    /**
     * Logs business events and connections (Audit trail).
     * @param message the message to log
     */
    public static synchronized void info(String message) {
        log(AUDIT_LOG, "INFO", message, null);
    }

    /**
     * Logs warnings.
     * @param message the warning message
     */
    public static synchronized void warn(String message) {
        log(AUDIT_LOG, "WARN", message, null);
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
