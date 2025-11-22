package utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // ANSI Color Codes
    private static final String RESET = "\033[0m";
    private static final String GREEN = "\033[0;32m";
    private static final String CYAN = "\033[0;36m";
    private static final String YELLOW = "\033[1;33m";
    private static final String RED = "\033[0;31m";
    private static final String WHITE = "\033[1;37m";
    private static final String DIM = "\033[2m";
    private static final String BRIGHT_BLUE = "\033[1;34m";
    
    public enum Level { DEBUG, INFO, WARN, ERROR }
    
    private static Level currentLevel = Level.INFO;
    
    static {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "echo", "off").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            // Fallback silently
        }
    }
    
    public static void setLevel(Level level) {
        currentLevel = level;
    }
    
    public static void debug(String tag, String message) {
        if (currentLevel.ordinal() <= Level.DEBUG.ordinal()) {
            log("DEBUG", tag, message);
        }
    }
    
    public static void info(String tag, String message) {
        if (currentLevel.ordinal() <= Level.INFO.ordinal()) {
            log("INFO", tag, message);
        }
    }
    
    public static void warn(String tag, String message) {
        if (currentLevel.ordinal() <= Level.WARN.ordinal()) {
            log("WARN", tag, message);
        }
    }
    
    public static void error(String tag, String message) {
        log("ERROR", tag, message);
    }
    
    public static void error(String tag, String message, Throwable e) {
        log("ERROR", tag, message + " - " + e.getMessage());
    }
    
    private static void log(String level, String tag, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String levelColor = getLevelColor(level);
        String tagColor = getTagColor(level);
        
        System.out.println(String.format("%s[%s]%s %s[%s]%s %s[%s]%s %s%s%s", 
            DIM, timestamp, RESET,
            levelColor, level, RESET,
            tagColor, tag, RESET,
            WHITE, message, RESET));
    }
    
    private static String getLevelColor(String level) {
        switch (level) {
            case "DEBUG": return CYAN;
            case "INFO":  return GREEN;
            case "WARN":  return YELLOW;
            case "ERROR": return RED;
            default:      return RESET;
        }
    }
    
    private static String getTagColor(String level) {
        switch (level) {
            case "DEBUG": return BRIGHT_BLUE;
            case "INFO":  return CYAN;
            case "WARN":  return YELLOW;
            case "ERROR": return RED;
            default:      return RESET;
        }
    }
}
