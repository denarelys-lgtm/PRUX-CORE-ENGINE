package com.example.detectcamera.core;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Recuperación limitada por componente, con backoff y sin reiniciar servicios Android a ciegas. */
public final class PruxRecoveryManager {
    public interface RecoveryHandler { boolean recover() throws Exception; }
    private static final String TAG = "PruxRecovery";
    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_DELAY_MS = 5_000L;
    private final PruxCore core;
    private final Map<String, RecoveryHandler> handlers = new HashMap<>();
    private final Map<String, Integer> attempts = new HashMap<>();
    private final Map<String, Long> lastAttempt = new HashMap<>();
    private final Set<String> pending = new HashSet<>();
    private final Set<String> exhausted = new HashSet<>();

    PruxRecoveryManager(PruxCore core) { this.core = core; }

    public synchronized void register(String component, RecoveryHandler handler) {
        if (component == null || component.trim().isEmpty() || handler == null) return;
        handlers.put(component, handler);
        attempts.remove(component);
        lastAttempt.remove(component);
        pending.remove(component);
        exhausted.remove(component);
    }

    public synchronized void unregister(String component) {
        handlers.remove(component);
        attempts.remove(component);
        lastAttempt.remove(component);
        pending.remove(component);
        exhausted.remove(component);
    }

    /** Solicita una recuperación. La ejecución siempre ocurre en el executor del Core. */
    public void request(String component, String reason) {
        final RecoveryHandler handler;
        final int attempt;
        final long delay;
        synchronized (this) {
            handler = handlers.get(component);
            if (handler == null) {
                core.publish("RECOVERY_UNAVAILABLE", "recovery", data(component, reason, 0));
                return;
            }
            if (pending.contains(component) || exhausted.contains(component)) return;
            int current = attempts.containsKey(component) ? attempts.get(component) : 0;
            if (current >= MAX_ATTEMPTS) { exhausted.add(component); return; }
            attempt = current + 1;
            attempts.put(component, attempt);
            pending.add(component);
            delay = BASE_DELAY_MS * (1L << (attempt - 1));
            lastAttempt.put(component, System.currentTimeMillis());
        }
        core.publish("RECOVERY_SCHEDULED", "recovery", data(component, reason, attempt));
        core.schedule(() -> {
            core.publish("RECOVERY_STARTED", "recovery", data(component, reason, attempt));
            boolean recovered = false;
            try { recovered = handler.recover(); }
            catch (Throwable error) { Log.e(TAG, "Recuperación fallida: " + component, error); }
            synchronized (PruxRecoveryManager.this) { pending.remove(component); }
            if (recovered) {
                synchronized (PruxRecoveryManager.this) { attempts.remove(component); lastAttempt.remove(component); exhausted.remove(component); }
                core.setState(component, "RUNNING");
                core.publish("RECOVERY_SUCCEEDED", "recovery", data(component, reason, attempt));
            } else {
                core.publish("RECOVERY_FAILED", "recovery", data(component, reason, attempt));
                if (attempt < MAX_ATTEMPTS) request(component, reason);
                else {
                    synchronized (PruxRecoveryManager.this) { exhausted.add(component); }
                    core.publish("RECOVERY_GIVE_UP", "recovery", data(component, reason, attempt));
                }
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public synchronized int getAttempts(String component) {
        Integer value = attempts.get(component); return value == null ? 0 : value;
    }

    private static Map<String,String> data(String component, String reason, int attempt) {
        Map<String,String> map = new HashMap<>();
        map.put("component", component == null ? "unknown" : component);
        map.put("reason", reason == null ? "unspecified" : reason);
        map.put("attempt", String.valueOf(attempt));
        return map;
    }
}
