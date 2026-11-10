package com.example.detectcamera.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Evento inmutable publicado entre subsistemas de Prux. */
public final class PruxEvent {
    private final String type;
    private final String source;
    private final long timestamp;
    private final Map<String, String> data;

    public PruxEvent(String type, String source, Map<String, String> data) {
        this.type = type == null ? "UNKNOWN" : type;
        this.source = source == null ? "unknown" : source;
        this.timestamp = System.currentTimeMillis();
        this.data = Collections.unmodifiableMap(data == null
                ? new HashMap<String, String>() : new HashMap<>(data));
    }

    public String getType() { return type; }
    public String getSource() { return source; }
    public long getTimestamp() { return timestamp; }
    public Map<String, String> getData() { return data; }
}
