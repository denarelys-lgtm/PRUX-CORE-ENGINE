package com.example.detectcamera.core;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Núcleo de coordinación de Prux. No sustituye los servicios actuales ni los
 * arranca por su cuenta: ofrece eventos, estado, tareas y diagnóstico común.
 */
public final class PruxCore {
    private static final String TAG = "PruxCore";
    private static volatile PruxCore instance;
    private final Context appContext;
    private final PruxEventBus eventBus = new PruxEventBus();
    private final PruxModuleRegistry moduleRegistry = new PruxModuleRegistry();
    private final PruxRecoveryManager recoveryManager = new PruxRecoveryManager(this);
    private final ScheduledThreadPoolExecutor executor;
    private final Map<String, String> states = new HashMap<>();
    private final AtomicInteger taskIds = new AtomicInteger();
    private volatile boolean initialized;
    private volatile ScheduledFuture<?> healthTask;
    private volatile ScheduledFuture<?> supervisionTask;

    private PruxCore(Context context) {
        appContext = context.getApplicationContext();
        executor = new ScheduledThreadPoolExecutor(2, new ThreadFactory() {
            private final AtomicInteger number = new AtomicInteger();
            @Override public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "PruxCore-" + number.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });
        executor.setRemoveOnCancelPolicy(true);
    }

    public static PruxCore initialize(Context context) {
        if (context == null) throw new IllegalArgumentException("Context no puede ser null");
        if (instance == null) {
            synchronized (PruxCore.class) {
                if (instance == null) instance = new PruxCore(context);
            }
        }
        instance.start();
        return instance;
    }
    public static PruxCore get() { return instance; }

    private synchronized void start() {
        if (initialized) return;
        registerBuiltInModules();
        initialized = true;
        setState("core", "RUNNING");
        healthTask = executor.scheduleWithFixedDelay(this::sampleHealth, 15, 30, TimeUnit.SECONDS);
        supervisionTask = executor.scheduleWithFixedDelay(this::supervise, 20, 20, TimeUnit.SECONDS);
        publish("CORE_READY", "core", null);
        Log.i(TAG, "PRUX CORE inicializado; " + moduleRegistry.getDescriptors().size() + " subsistemas registrados");
    }

    private void registerBuiltInModules() {
        moduleRegistry.registerDescriptor("adb", "ADB Engine", "Conexión, emparejamiento y monitorización ADB", "adb");
        moduleRegistry.registerDescriptor("camera", "Camera Engine", "Cámara y servicio de cámara", "camera");
        moduleRegistry.registerDescriptor("capture", "Capture Engine", "MediaProjection y captura de pantalla", "capture");
        moduleRegistry.registerDescriptor("network", "Network Engine", "Servidor HTTP y conectividad", "network");
        moduleRegistry.registerDescriptor("audio", "Audio Engine", "Captura y transmisión de audio", "audio");
        moduleRegistry.registerDescriptor("automation", "Automation Engine", "Punto de integración para automatizaciones", "automation");
        for (PruxModuleRegistry.Descriptor descriptor : moduleRegistry.getDescriptors()) {
            setState(descriptor.statusKey, "REGISTERED");
        }
    }

    public Context getApplicationContext() { return appContext; }
    public PruxEventBus events() { return eventBus; }
    public PruxModuleRegistry modules() { return moduleRegistry; }
    public PruxRecoveryManager recovery() { return recoveryManager; }
    public boolean isInitialized() { return initialized; }

    public void publish(String type, String source, Map<String, String> data) {
        eventBus.publish(new PruxEvent(type, source, data));
    }
    public void setState(String key, String value) {
        if (key == null) return;
        synchronized (states) { states.put(key, value == null ? "UNKNOWN" : value); }
        Map<String, String> data = new HashMap<>();
        data.put("key", key); data.put("value", value == null ? "UNKNOWN" : value);
        publish("STATE_CHANGED", "core", data);
    }
    public String getState(String key) {
        synchronized (states) { return states.get(key); }
    }
    public Map<String, String> getStateSnapshot() {
        synchronized (states) { return Collections.unmodifiableMap(new HashMap<>(states)); }
    }

    /** Devuelve un diagnóstico seguro del estado y consumo de recursos de Prux. */
    public Map<String, String> diagnostics() {
        return PruxDiagnostics.snapshot(appContext, getStateSnapshot());
    }

    /** Marca un componente y solicita recuperación solo cuando su estado es ERROR. */
    public void reportComponent(String component, String state, String event, Map<String,String> data) {
        setState(component, state);
        if (event != null) publish(event, component, data);
        if ("ERROR".equalsIgnoreCase(state) || "FAILED".equalsIgnoreCase(state)) {
            recoveryManager.request(component, event == null ? state : event);
        }
    }

    private void supervise() {
        try {
            Map<String,String> snapshot = getStateSnapshot();
            for (Map.Entry<String,String> entry : snapshot.entrySet()) {
                String value = entry.getValue();
                if ("ERROR".equalsIgnoreCase(value) || "FAILED".equalsIgnoreCase(value)) {
                    recoveryManager.request(entry.getKey(), "supervisor-detected-" + value);
                }
            }
            publish("SUPERVISION_CYCLE", "health-monitor", singleton("components", String.valueOf(snapshot.size())));
        } catch (Throwable error) { Log.w(TAG, "Error supervisando subsistemas", error); }
    }


    /** Ejecuta trabajo fuera del hilo principal. Devuelve un identificador de tarea. */
    public int execute(Runnable task) {
        if (task == null) throw new IllegalArgumentException("task null");
        final int id = taskIds.incrementAndGet();
        executor.execute(() -> {
            publish("TASK_STARTED", "scheduler", singleton("taskId", String.valueOf(id)));
            try { task.run(); publish("TASK_COMPLETED", "scheduler", singleton("taskId", String.valueOf(id))); }
            catch (Throwable error) {
                Log.e(TAG, "Tarea " + id + " falló", error);
                publish("TASK_FAILED", "scheduler", singleton("taskId", String.valueOf(id)));
            }
        });
        return id;
    }
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        if (task == null || unit == null) throw new IllegalArgumentException("task/unit null");
        return executor.schedule(task, Math.max(0L, delay), unit);
    }
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        if (task == null || unit == null || period <= 0) throw new IllegalArgumentException("parámetros inválidos");
        return executor.scheduleAtFixedRate(task, Math.max(0L, initialDelay), period, unit);
    }

    private void sampleHealth() {
        try {
            ActivityManager manager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
            if (manager != null) manager.getMemoryInfo(memory);
            Runtime runtime = Runtime.getRuntime();
            long usedHeap = runtime.totalMemory() - runtime.freeMemory();
            Map<String, String> data = new HashMap<>();
            data.put("availableMemoryBytes", String.valueOf(memory.availMem));
            data.put("lowMemory", String.valueOf(memory.lowMemory));
            data.put("appHeapUsedBytes", String.valueOf(usedHeap));
            data.put("nativeHeapBytes", String.valueOf(Debug.getNativeHeapAllocatedSize()));
            publish("HEALTH_SAMPLE", "health-monitor", data);
            if (memory.lowMemory) {
                publish("LOW_MEMORY", "health-monitor", data);
                // No mata ni reinicia motores: informa a los módulos para que reduzcan carga.
                publish("RESOURCE_PRESSURE", "health-monitor", data);
            }
        } catch (Throwable error) {
            Log.w(TAG, "No se pudo tomar muestra de salud", error);
        }
    }

    private static Map<String, String> singleton(String key, String value) {
        Map<String, String> map = new HashMap<>(); map.put(key, value); return map;
    }
}
