package server;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import utils.Logger;

public class TimeoutManager {
    private static final String TAG = "Timeout";
    
    private final Map<ConnectionHandler, Long> connections;
    private final long timeoutMs;
    
    public TimeoutManager(int timeoutSeconds) {
        this.connections = new ConcurrentHashMap<>();
        this.timeoutMs = timeoutSeconds * 1000L;
        Logger.info(TAG, "Timeout set to " + timeoutSeconds + " seconds");
    }
    
    public void register(ConnectionHandler handler) {
        connections.put(handler, System.currentTimeMillis());
    }
    
    public void unregister(ConnectionHandler handler) {
        connections.remove(handler);
    }
    
    public void checkTimeouts() {
        Iterator<Map.Entry<ConnectionHandler, Long>> it = connections.entrySet().iterator();
        
        while (it.hasNext()) {
            Map.Entry<ConnectionHandler, Long> entry = it.next();
            ConnectionHandler handler = entry.getKey();
            
            if (handler.getState() == ConnectionHandler.State.CLOSED) {
                it.remove();
                continue;
            }
            
            if (handler.isTimedOut(timeoutMs)) {
                Logger.warn(TAG, "Connection timed out: " + handler.getRemoteAddress() + 
                           " (duration: " + handler.getConnectionDuration() + "ms)");
                handler.close();
                it.remove();
            }
        }
    }
    
    public int getActiveConnections() {
        return connections.size();
    }
}