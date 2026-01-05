package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SonicLogger {
    
    // Colors
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String GRAY = "\u001B[38;5;242m";
    private static final String CYAN = "\u001B[38;5;39m";
    private static final String BRIGHT_CYAN = "\u001B[38;5;51m";
    private static final String GREEN = "\u001B[38;5;46m";
    private static final String YELLOW = "\u001B[38;5;226m";
    private static final String RED = "\u001B[38;5;196m";
    private static final String PURPLE = "\u001B[38;5;147m";
    
    private final String className;
    private final DateTimeFormatter timeFormatter;
    
    public SonicLogger(Class<?> clazz) {
        this.className = clazz.getName();
        this.timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    }
    
    public SonicLogger(String name) {
        this.className = name;
        this.timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    }
    
    // Logging methods
    public void trace(String message) { log(GRAY, "TRACE", message); }
    public void debug(String message) { log(CYAN, "DEBUG", message); }
    public void info(String message) { log(BRIGHT_CYAN, "INFO", message); }
    public void success(String message) { log(GREEN, "SUCCESS", message); }
    public void warn(String message) { log(YELLOW, "WARN", message); }
    public void error(String message) { log(RED, "ERROR", message); }
    
    public void error(String message, Throwable throwable) {
        error(message);
        if (throwable != null) {
            System.err.println(DIM + "    ↳ " + RED + throwable.getClass().getSimpleName() + 
                             RESET + DIM + ": " + throwable.getMessage() + RESET);
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            int limit = Math.min(5, stackTrace.length);
            for (int i = 0; i < limit; i++) {
                System.err.println(DIM + "      at " + stackTrace[i] + RESET);
            }
            if (stackTrace.length > 5) {
                System.err.println(DIM + "      ... " + (stackTrace.length - 5) + " more" + RESET);
            }
        }
    }
    
    private void log(String color, String levelText, String message) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        long pid = ProcessHandle.current().pid();
        String threadName = Thread.currentThread().getName();
        String shortClassName = abbreviateClassName(className);
        
        System.out.println(
            GRAY + timestamp + RESET + " " +
            BOLD + color + String.format("%-7s", levelText) + RESET + " " +
            GRAY + String.format("%-5s", pid) + RESET + " " +
            DIM + "---" + RESET + " " +
            DIM + "[" + threadName + "]" + RESET + " " +
            PURPLE + shortClassName + RESET + " " +
            DIM + ":" + RESET + " " +
            message
        );
    }
    
    private String abbreviateClassName(String className) {
        if (className.length() <= 40) return className;
        String[] parts = className.split("\\.");
        if (parts.length == 1) return className;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            sb.append(parts[i].charAt(0)).append(".");
        }
        sb.append(parts[parts.length - 1]);
        return sb.toString();
    }
    
    public static SonicLogger getLogger(Class<?> clazz) { return new SonicLogger(clazz); }
    public static SonicLogger getLogger(String name) { return new SonicLogger(name); }
}