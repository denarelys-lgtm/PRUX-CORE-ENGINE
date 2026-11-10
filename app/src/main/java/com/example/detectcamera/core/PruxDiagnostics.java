package com.example.detectcamera.core;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Snapshot ligero para diagnóstico; no recopila contenido personal ni credenciales. */
public final class PruxDiagnostics {
    private PruxDiagnostics() {}

    public static Map<String, String> snapshot(Context context, Map<String, String> states) {
        Map<String, String> result = new HashMap<>();
        result.put("timestamp", String.valueOf(System.currentTimeMillis()));
        result.put("stateCount", String.valueOf(states == null ? 0 : states.size()));
        if (states != null) for (Map.Entry<String,String> entry : states.entrySet()) {
            result.put("component." + entry.getKey(), entry.getValue());
        }
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
            if (manager != null) {
                manager.getMemoryInfo(memory);
                result.put("memory.availableBytes", String.valueOf(memory.availMem));
                result.put("memory.low", String.valueOf(memory.lowMemory));
                result.put("memory.thresholdBytes", String.valueOf(memory.threshold));
            }
            Runtime runtime = Runtime.getRuntime();
            result.put("heap.usedBytes", String.valueOf(runtime.totalMemory() - runtime.freeMemory()));
            result.put("heap.maxBytes", String.valueOf(runtime.maxMemory()));
            result.put("heap.nativeBytes", String.valueOf(Debug.getNativeHeapAllocatedSize()));
        } catch (Throwable ignored) { result.put("diagnostics.error", ignored.getClass().getSimpleName()); }
        return Collections.unmodifiableMap(result);
    }
}
