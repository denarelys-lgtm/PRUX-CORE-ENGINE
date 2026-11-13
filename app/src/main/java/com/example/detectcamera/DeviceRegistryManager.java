
package com.example.detectcamera;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class DeviceRegistryManager {

    private static final String TAG = "PruxRegistry";

    private static final String DATABASE_URL =
            "https://detectcameracentral-default-rtdb.firebaseio.com/";

    private static final String DATABASE_NODE = "dispositivos";

    public static void registrarDispositivoEnNube(
            Context context,
            String urlTunel
    ) {

        if (context == null) {
            Log.e(TAG, "Context inválido.");
            return;
        }

        if (urlTunel == null || urlTunel.trim().isEmpty()) {
            Log.e(TAG, "URL de Cloudflare vacía. Registro cancelado.");
            return;
        }

        try {

            Context appContext = context.getApplicationContext();

            String modeloDispositivo = Build.MODEL;

            String idUnico = Settings.Secure.getString(
                    appContext.getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );

            if (idUnico == null || idUnico.isEmpty()) {
                Log.e(TAG, "No se pudo obtener ANDROID_ID.");
                return;
            }

            // Conexión explícita con Firebase Realtime Database.
            FirebaseDatabase firebaseDatabase =
                    FirebaseDatabase.getInstance(DATABASE_URL);

            DatabaseReference databaseReference =
                    firebaseDatabase
                            .getReference(DATABASE_NODE)
                            .child(idUnico);

            // Preparar los datos del dispositivo.
            Map<String, Object> datos = new HashMap<>();

            datos.put("modelo", modeloDispositivo);
            datos.put("url", urlTunel.trim());
            datos.put("timestamp", ServerValue.TIMESTAMP);
            datos.put("estado", "conectado");
            datos.put("plataforma", "Android");

            // Registrar todos los datos en una sola operación.
            databaseReference.updateChildren(
                    datos,
                    (DatabaseError error, DatabaseReference reference) -> {

                        if (error != null) {

                            Log.e(
                                    TAG,
                                    "ERROR AL REGISTRAR EN FIREBASE: "
                                            + error.getMessage()
                            );

                            Log.e(
                                    TAG,
                                    "Código de error: " + error.getCode()
                            );

                            Log.e(
                                    TAG,
                                    "Detalles: " + error.getDetails()
                            );

                            return;
                        }

                        Log.i(
                                TAG,
                                "REGISTRO CONFIRMADO EN FIREBASE"
                        );

                        Log.i(
                                TAG,
                                "Dispositivo: " + modeloDispositivo
                        );

                        Log.i(
                                TAG,
                                "ID: " + idUnico
                        );

                        Log.i(
                                TAG,
                                "Cloudflare: " + urlTunel
                        );
                    }
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Error iniciando el registro en Firebase.",
                    e
            );
        }
    }
}
