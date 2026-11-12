package com.example.detectcamera;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

/** Compatibility name retained so the rest of the project does not need a large refactor. */
public final class PruxBackgroundBypass {

    private static final String TAG = "PruxBackgroundBypass";

    private PruxBackgroundBypass() {}

    public static void aplicarExencionesBackground(Context context) {
        try {
            // 1. Ejecutar el puente privilegiado existente (comandos de sistema / ADB / Shizuku)
            PruxPrivilegedBridge.applyBackgroundExemptions(context);
            Log.i(TAG, "Exenciones de fondo aplicadas mediante PruxPrivilegedBridge.");
        } catch (Exception e) {
            Log.e(TAG, "Error aplicando exenciones con el puente privilegiado", e);
        }

        // 2. Respaldo nativo de Android para ignorar optimizaciones de batería si aplica
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                String packageName = context.getPackageName();
                
                if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    Log.i(TAG, "Solicitud nativa de exención de batería lanzada.");
                }
            } catch (Exception e) {
                Log.w(TAG, "No se pudo lanzar el diálogo nativo de optimización de batería (puede requerir interacción o contexto de UI).", e);
            }
        }
    }
}
