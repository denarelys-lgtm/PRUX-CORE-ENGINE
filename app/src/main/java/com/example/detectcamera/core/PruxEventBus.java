package com.example.detectcamera.core;

import android.util.Log;
import java.util.concurrent.CopyOnWriteArrayList;

/** Bus ligero; los listeners se ejecutan en el hilo que publica el evento. */
public final class PruxEventBus {
    public interface Listener { void onEvent(PruxEvent event); }
    private static final String TAG = "PruxEventBus";
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(Listener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }
    public void unsubscribe(Listener listener) { listeners.remove(listener); }
    public void publish(PruxEvent event) {
        if (event == null) return;
        for (Listener listener : listeners) {
            try { listener.onEvent(event); }
            catch (Throwable error) { Log.e(TAG, "Listener falló para " + event.getType(), error); }
        }
    }
    public int listenerCount() { return listeners.size(); }
}
