# PRUX CORE ENGINE – primera integración conservadora

## Qué se añadió
- `PruxApplication`: inicializa el Core al crear el proceso.
- `core/PruxCore`: singleton de coordinación, estado compartido, ejecutor de tareas y muestreo de salud.
- `core/PruxEventBus` y `PruxEvent`: comunicación desacoplada por eventos.
- `core/PruxModuleRegistry` y `PruxModule`: registro descriptivo y contrato para futuros módulos.
- Integración no invasiva de eventos/estados con `ServerService`, `CameraService` y `BootReceiver`.

## Compatibilidad y preservación
Los motores originales, Activities, Services, permisos, dependencias, recursos, rutas HTTP y lógica ADB/cámara/captura no se han sustituido. El Core no inicia ni detiene los servicios existentes, no cambia los permisos y no intenta reiniciar procesos automáticamente. La integración es aditiva: si el Core no se inicializa por alguna excepción, la aplicación continúa con sus componentes anteriores.

## API breve
```java
PruxCore core = PruxCore.get();
if (core != null) {
    core.publish("MY_EVENT", "MyModule", null);
    core.setState("my-module", "RUNNING");
    String state = core.getState("network");
    int taskId = core.execute(() -> {
        // Trabajo corto/no UI
    });
}
```

Suscribirse a eventos:
```java
PruxCore core = PruxCore.get();
PruxEventBus.Listener listener = event -> {
    // No bloquear; el callback corre en el hilo que publica el evento.
};
if (core != null) core.events().subscribe(listener);
// Al dejar de necesitarlo: core.events().unsubscribe(listener);
```

## Límites de esta primera etapa
- El registro describe subsistemas; todavía no carga código externo ni plugins arbitrarios.
- El monitor emite muestras de memoria/heap cada 30 segundos; no promete evitar cierres de Android ni modifica límites del sistema.
- La ejecución de tareas es interna al proceso y se pierde si Android mata el proceso.
- La recuperación automática se deja para una segunda etapa, con políticas específicas por servicio para no provocar bucles de reinicio ni romper MediaProjection, permisos o restricciones de foreground services.

## Verificación
Compilar en Android Studio con el Gradle/SDK configurado por el proyecto. Probar primero instalación/arranque, acceso a la interfaz, ADB, servidor en el puerto 8080, cámara y MediaProjection. Esta modificación no incluye una compilación APK en este entorno.
