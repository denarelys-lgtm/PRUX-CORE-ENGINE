package com.example.detectcamera;

import android.app.Application;
import android.util.Log;
import com.example.detectcamera.core.PruxCore;

/** Inicializa el núcleo sin alterar el ciclo de vida de los servicios existentes. */
public final class PruxApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        try {
            PruxCore.initialize(this);
            Log.i("PruxApplication", "PRUX CORE disponible");
        } catch (Throwable error) {
            // Un fallo del núcleo no debe impedir abrir las funciones originales.
            Log.e("PruxApplication", "No se pudo inicializar PRUX CORE; continúan los componentes originales", error);
        }
    }
}
