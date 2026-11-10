# PRUX CORE ENGINE — Etapa 2

## Incorporado
- Supervisor periódico de estados de subsistemas.
- Diagnóstico de memoria Java/nativa y memoria disponible del sistema.
- Gestor de recuperación por componente con backoff (5 s, 10 s y 20 s) y máximo de tres intentos por ciclo.
- Recuperación del servidor HTTP únicamente desde `ServerService` mientras el servicio permanezca vivo.
- Publicación al Core de transiciones ADB y de captura MediaProjection.
- Eventos `RESOURCE_PRESSURE`, `SUPERVISION_CYCLE` y eventos `RECOVERY_*`.

## Seguridad de ciclo de vida
El Core no intenta iniciar servicios foreground desde segundo plano ni recrear sesiones MediaProjection sin consentimiento del usuario. ADB conserva su mecanismo de reconexión original. La recuperación del servidor HTTP está limitada al ciclo de vida de `ServerService`. Los fallos no detienen los componentes originales.

## Estados consultables
`PruxCore.get().getStateSnapshot()` devuelve una copia inmutable del estado.
`PruxCore.get().diagnostics()` devuelve una instantánea de diagnóstico sin credenciales ni contenido personal.

## Eventos principales
`ADB_CONNECTED`, `ADB_DISCONNECTED`, `SCREEN_CAPTURE_STARTED`, `SCREEN_CAPTURE_STOPPED`, `LOW_MEMORY`, `RESOURCE_PRESSURE`, `SUPERVISION_CYCLE`, `RECOVERY_SCHEDULED`, `RECOVERY_STARTED`, `RECOVERY_SUCCEEDED`, `RECOVERY_FAILED`, `RECOVERY_GIVE_UP`.

## Compilación
Proyecto Android Java 8, compileSdk 34, minSdk 26. Compilar con el Gradle Wrapper del proyecto en Android Studio.
