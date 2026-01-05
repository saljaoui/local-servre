package session;

import http.model.HttpRequest;
import http.model.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();
    private final Map<String, Map<String, String>> sessions = new ConcurrentHashMap<>();

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void attachSession(HttpRequest request) {
        String sessionId = request.getCookie(CookieUtil.SESSION_COOKIE);
        Map<String, String> sessionData = null;

        if (sessionId != null) {
            sessionData = sessions.get(sessionId);
        }

        boolean created = false;
        if (sessionData == null) {
            sessionId = CookieUtil.generateSessionId();
            sessionData = new ConcurrentHashMap<>();
            sessions.put(sessionId, sessionData);
            created = true;
        }

        request.setSessionId(sessionId);
        request.setSessionData(sessionData);
        request.setNewSession(created);
    }

    public void appendSessionCookie(HttpRequest request, HttpResponse response) {
        if (request.isNewSession()) {
            response.addHeader("Set-Cookie", CookieUtil.buildSessionCookie(request.getSessionId()));
        }
    }
}
