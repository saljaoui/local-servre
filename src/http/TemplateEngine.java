package http;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class TemplateEngine {

    public static String render(String templatePath, Map<String, String> data) {
        try {
            // 1. Load the file
            String content = new String(Files.readAllBytes(Paths.get(templatePath)));

            // 2. Replace placeholders (e.g., {{title}})
            if (data != null) {
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    String key = "{{" + entry.getKey() + "}}";
                    String val = entry.getValue() == null ? "" : entry.getValue();
                    content = content.replace(key, val);
                }
            }
            return content;
        } catch (Exception e) {
            return "Error loading template: " + e.getMessage();
        }
    }
}