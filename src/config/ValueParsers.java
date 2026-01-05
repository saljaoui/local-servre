package config;

import java.util.*;

public class ValueParsers {
    
    public static Map<String, String> parseStringMap(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> fields = JsonParser.splitTopLevel(json);
        
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String value = parseString(entry.getValue());
            map.put(entry.getKey(), value);
        }
        
        return map;
    }
    
    public static List<String> parseStringArray(String json) {
        List<String> list = new ArrayList<>();
        json = json.substring(1, json.length() - 1).trim();
        
        List<String> items = JsonParser.splitArray(json);
        
        for (String item : items) {
            list.add(parseString(item));
        }
        
        return list;
    }
    
    public static String parseString(String json) {
        json = json.trim();
        if (json.startsWith("\"") && json.endsWith("\"")) {
            return json.substring(1, json.length() - 1);
        }
        return json;
    }
    
    public static int parseInt(String json) {
        return Integer.parseInt(json.trim());
    }
    
    public static long parseLong(String json) {
        return Long.parseLong(json.trim());
    }
    
    public static boolean parseBoolean(String json) {
        return Boolean.parseBoolean(json.trim());
    }
}

