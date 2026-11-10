package com.example.detectcamera.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registro descriptivo; no inicia ni detiene servicios Android existentes. */
public final class PruxModuleRegistry {
    public static final class Descriptor {
        public final String id;
        public final String name;
        public final String description;
        public final String statusKey;
        Descriptor(String id, String name, String description, String statusKey) {
            this.id = id; this.name = name; this.description = description; this.statusKey = statusKey;
        }
    }

    private final Map<String, Descriptor> descriptors = new LinkedHashMap<>();
    private final Map<String, PruxModule> modules = new LinkedHashMap<>();

    public synchronized void registerDescriptor(String id, String name, String description, String statusKey) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("id vacío");
        descriptors.put(id, new Descriptor(id, name, description, statusKey));
    }
    public synchronized boolean register(PruxModule module) {
        if (module == null || module.getId() == null) return false;
        if (modules.containsKey(module.getId())) return false;
        modules.put(module.getId(), module);
        return true;
    }
    public synchronized PruxModule get(String id) { return modules.get(id); }
    public synchronized List<Descriptor> getDescriptors() {
        return Collections.unmodifiableList(new ArrayList<>(descriptors.values()));
    }
    public synchronized int size() { return descriptors.size() + modules.size(); }
}
