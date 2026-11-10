package com.example.detectcamera.core;

/** Contrato opcional para futuros módulos administrados por PruxCore. */
public interface PruxModule {
    String getId();
    default void onCoreReady(PruxCore core) {}
    default void onCoreShutdown() {}
}
