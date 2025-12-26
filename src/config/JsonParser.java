package config;



import java.util.*;

/**
 * Core JSON parsing utilities - handles the tricky character-by-character parsing
 * Reusable for other JSON parsing needs
 */
public class JsonParser {
    
    public static Map<String, String> splitTopLevel(String json) {
        Map<String, String> result = new HashMap<>();
        int i = 0;
        while (i < json.length()) {
            // Skip whitespace
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;
            
            // Read key
            if (json.charAt(i) != '"') break;
            i++; // skip opening quote
            int keyStart = i;
            while (i < json.length() && json.charAt(i) != '"') i++;
            String key = json.substring(keyStart, i);
            i++; // skip closing quote
            
            // Skip to colon
            while (i < json.length() && json.charAt(i) != ':') i++;
            i++; // skip colon
            
            // Skip whitespace
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            
            // Read value
            int valueStart = i;
            i = skipValue(json, i);
            String value = json.substring(valueStart, i).trim();
            
            result.put(key, value);
            // Skip comma
            while (i < json.length() && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) i++;
        }
        
        return result;
    }
    
    public static List<String> splitArray(String json) {
        List<String> items = new ArrayList<>();
        int i = 0;
        
        while (i < json.length()) {
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;
            
            int start = i;
            i = skipValue(json, i);
            items.add(json.substring(start, i).trim());
            
            while (i < json.length() && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) i++;
        }
        
        return items;
    }
    
    public static int skipValue(String json, int start) {
        char c = json.charAt(start);
        
        if (c == '{') {
            return skipObject(json, start);
        } else if (c == '[') {
            return skipArray(json, start);
        } else if (c == '"') {
            return skipString(json, start);
        } else {
            // Number, boolean, or null
            int i = start;
            while (i < json.length() && json.charAt(i) != ',' && 
                   json.charAt(i) != '}' && json.charAt(i) != ']') {
                i++;
            }
            return i;
        }
    }
    
    private static int skipObject(String json, int start) {
        int i = start + 1; // skip opening brace
        int depth = 1;
        
        while (i < json.length() && depth > 0) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            else if (c == '"') i = skipString(json, i) - 1;
            i++;
        }
        return i;
    }
    
    private static int skipArray(String json, int start) {
        int i = start + 1; // skip opening bracket
        int depth = 1;
        
        while (i < json.length() && depth > 0) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') depth--;
            else if (c == '"') i = skipString(json, i) - 1;
            else if (c == '{') i = skipObject(json, i) - 1;
            i++;
        }
        return i;
    }
    
    private static int skipString(String json, int start) {
        int i = start + 1; // skip opening quote
        while (i < json.length()) {
            if (json.charAt(i) == '"' && json.charAt(i - 1) != '\\') {
                return i + 1;
            }
            i++;
        }
        return i;
    }
}
