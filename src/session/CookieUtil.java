package session;

import java.security.SecureRandom;
import java.util.Base64;

public class CookieUtil {

    public static final String SESSION_COOKIE = "SID";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateSessionId() {
        byte[] bytes = new byte[18]; // 24 char base64 string
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String buildSessionCookie(String sessionId) {
        // Minimal, server-wide session cookie
        return SESSION_COOKIE + "=" + sessionId + "; Path=/; HttpOnly";
    }
}
