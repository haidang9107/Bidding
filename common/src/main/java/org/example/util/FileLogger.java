package org.example.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * High-performance, Thread-safe FileLogger with Daily Rotation and Console Colors.
 */
public class FileLogger {
    // ANSI Color Codes for Console
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";

    private static final String LOG_DIR;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Async Logging Queue
    private static final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>();
    private static final Thread logWorker;

    static {
        // 1. Locate Log Directory
        String userDir = System.getProperty("user.dir");
        File projectRoot = findProjectRoot(new File(userDir));
        String rootPath = projectRoot != null ? projectRoot.getAbsolutePath() : userDir;
        LOG_DIR = rootPath + File.separator + "common" + File.separator + "logs" + File.separator;

        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 2. Start Async Log Worker
        logWorker = new Thread(FileLogger::processQueue);
        logWorker.setDaemon(true);
        logWorker.setName("FileLogger-Worker");
        logWorker.start();
    }

    private static File findProjectRoot(File currentDir) {
        if (currentDir == null) return null;
        File env = new File(currentDir, ".env");
        if (env.exists()) return currentDir;
        return findProjectRoot(currentDir.getParentFile());
    }

    public static void info(String message) {
        enqueue("INFO", message, null);
    }

    public static void warn(String message) {
        enqueue("WARN", message, null);
    }

    public static void error(String message) {
        enqueue("ERROR", message, null);
    }

    public static void error(String message, Throwable throwable) {
        enqueue("ERROR", message, throwable);
    }

    public static void debug(String message) {
        enqueue("DEBUG", message, null);
    }

    private static void enqueue(String level, String message, Throwable throwable) {
        logQueue.offer(new LogEntry(LocalDateTime.now(), level, message, throwable, Thread.currentThread().getName()));
    }

    private static void processQueue() {
        while (true) {
            try {
                LogEntry entry = logQueue.take();
                writeLog(entry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void writeLog(LogEntry entry) {
        String dateStr = entry.timestamp.format(DATE_FORMAT);
        String timeStr = entry.timestamp.format(TIME_FORMAT);
        
        // Log files by date: app-2023-10-27.log
        String fileName = LOG_DIR + "app-" + dateStr + ".log";
        
        // 1. Write to File
        try (FileWriter fw = new FileWriter(fileName, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            out.printf("[%s] [%s] [%s] %s%n", timeStr, entry.level, entry.threadName, entry.message);
            if (entry.throwable != null) {
                entry.throwable.printStackTrace(out);
            }
        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to write to log file: " + e.getMessage());
        }

        // 2. Write to Console with Colors
        String color = switch (entry.level) {
            case "INFO" -> GREEN;
            case "WARN" -> YELLOW;
            case "ERROR" -> RED;
            case "DEBUG" -> BLUE;
            default -> RESET;
        };

        System.out.printf("%s[%s]%s %s[%s]%s %s[%s]%s %s%n", 
            PURPLE, timeStr, RESET,
            color, entry.level, RESET,
            BLUE, entry.threadName, RESET,
            entry.message);
            
        if (entry.throwable != null) {
            entry.throwable.printStackTrace(System.err);
        }
    }

    private record LogEntry(LocalDateTime timestamp, String level, String message, Throwable throwable, String threadName) {}
}
