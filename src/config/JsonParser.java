// src/config/JsonParser.java
package config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonParser {
    private final String s;
    private int i = 0;

    public JsonParser(String s) {
        if (s == null) throw new IllegalArgumentException("JSON input is null");
        this.s = s;
    }

    public Object parseValue() {
        skipWs();
        if (i >= s.length()) throw err("Unexpected end of JSON");

        char c = s.charAt(i);
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (c == 't' || c == 'f') return parseBoolean();
        if (c == 'n') return parseNull();
        if (c == '-' || isDigit(c)) return parseNumber();

        throw err("Unexpected character '" + c + "'");
    }

    private Map<String, Object> parseObject() {
        expect('{');
        skipWs();

        Map<String, Object> map = new LinkedHashMap<>();
        if (peek('}')) { i++; return map; }

        while (true) {
            skipWs();
            String key = parseString();
            skipWs();
            expect(':');
            Object val = parseValue();
            map.put(key, val);
            skipWs();

            if (peek('}')) { i++; break; }
            expect(',');
        }
        return map;
    }

    private List<Object> parseArray() {
        expect('[');
        skipWs();

        List<Object> list = new ArrayList<>();
        if (peek(']')) { i++; return list; }

        while (true) {
            list.add(parseValue());
            skipWs();

            if (peek(']')) { i++; break; }
            expect(',');
        }
        return list;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();

        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '"') return sb.toString();

            if (c == '\\') {
                if (i >= s.length()) throw err("Bad escape sequence");
                char e = s.charAt(i++);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u': {
                        if (i + 4 > s.length()) throw err("Bad \\u escape");
                        String hex = s.substring(i, i + 4);
                        i += 4;
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException ex) {
                            throw err("Bad \\u escape hex: " + hex);
                        }
                        break;
                    }
                    default:
                        throw err("Unknown escape: \\" + e);
                }
            } else {
                sb.append(c);
            }
        }

        throw err("Unterminated string");
    }

    private Boolean parseBoolean() {
        if (s.startsWith("true", i)) { i += 4; return true; }
        if (s.startsWith("false", i)) { i += 5; return false; }
        throw err("Invalid boolean");
    }

    private Object parseNull() {
        if (s.startsWith("null", i)) { i += 4; return null; }
        throw err("Invalid null");
    }

    private Number parseNumber() {
        int start = i;

        if (s.charAt(i) == '-') i++;

        if (i >= s.length() || !isDigit(s.charAt(i))) {
            throw err("Invalid number");
        }

        while (i < s.length() && isDigit(s.charAt(i))) i++;

        boolean isFloat = false;

        if (i < s.length() && s.charAt(i) == '.') {
            isFloat = true;
            i++;
            if (i >= s.length() || !isDigit(s.charAt(i))) throw err("Invalid number fraction");
            while (i < s.length() && isDigit(s.charAt(i))) i++;
        }

        if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            isFloat = true;
            i++;
            if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
            if (i >= s.length() || !isDigit(s.charAt(i))) throw err("Invalid number exponent");
            while (i < s.length() && isDigit(s.charAt(i))) i++;
        }

        String num = s.substring(start, i);
        try {
            return isFloat ? Double.parseDouble(num) : Long.parseLong(num);
        } catch (NumberFormatException e) {
            throw err("Invalid number: " + num);
        }
    }

    private void skipWs() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') i++;
            else break;
        }
    }

    private boolean peek(char c) {
        skipWs();
        return i < s.length() && s.charAt(i) == c;
    }

    private void expect(char c) {
        skipWs();
        if (i >= s.length()) throw err("Expected '" + c + "' but reached end");
        char got = s.charAt(i);
        if (got != c) throw err("Expected '" + c + "' but found '" + got + "'");
        i++;
    }

    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }

    private IllegalArgumentException err(String msg) {
        return new IllegalArgumentException(msg + " at position " + i);
    }
}
