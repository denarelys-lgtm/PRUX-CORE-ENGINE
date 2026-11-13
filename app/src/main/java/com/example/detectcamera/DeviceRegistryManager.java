package com.example.detectcamera;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DeviceRegistryManager {

    private static final String TAG = "PruxRegistry";

    public static void registrarDispositivoEnNube(Context context, String urlTunel) {
        try {
            String modeloDispositivo = Build.MODEL;
            String idUnico = android.provider.Settings.Secure.getString(
                    context.getContentResolver(), 
                    android.provider.Settings.Secure.ANDROID_ID
            );

            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("dispositivos");

            databaseReference.child(idUnico).child("modelo").setValue(modeloDispositivo);
            databaseReference.child(idUnico).child("url").setValue(urlTunel);
            databaseReference.child(idUnico).child("timestamp").setValue(System.currentTimeMillis());

            Log.i(TAG, "Dispositivo registrado en Firebase con éxito: " + modeloDispositivo + " -> " + urlTunel);
        } catch (Exception e) {
            Log.e(TAG, "Error al registrar el dispositivo en Firebase", e);
        }
    }
}
