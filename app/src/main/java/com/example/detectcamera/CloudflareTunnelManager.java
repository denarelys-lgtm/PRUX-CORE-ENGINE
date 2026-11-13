package com.example.detectcamera;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CloudflareTunnelManager {

    private static final String TAG = "PruxCloudflare";
    private static Process tunnelProcess = null;

    public static synchronized void iniciarTunel(Context context) {
        if (tunnelProcess != null) {
            Log.i(TAG, "El túnel ya está en ejecución.");
            return;
        }

        new Thread(() -> {
            try {
                File binDir = context.getFilesDir();
                File tunnelBin = new File(binDir, "cloudflared");

                // Si el binario extraído no existe en almacenamiento interno, lo descomprimimos del assets/cloudflared.zip
                if (!tunnelBin.exists()) {
                    Log.i(TAG, "Descomprimiendo cloudflared desde los assets (ZIP)...");
                    try (InputStream assetStream = context.getAssets().open("cloudflared.zip");
                         ZipInputStream zis = new ZipInputStream(assetStream)) {
                        
                        ZipEntry zipEntry;
                        while ((zipEntry = zis.getNextEntry()) != null) {
                            if (zipEntry.getName().equals("cloudflared") || zipEntry.getName().endsWith("cloudflared")) {
                                try (FileOutputStream fos = new FileOutputStream(tunnelBin)) {
                                    byte[] buffer = new byte[1024];
                                    int len;
                                    while ((len = zis.read(buffer)) > 0) {
                                        fos.write(buffer, 0, len);
                                    }
                                }
                                break;
                            }
                            zis.closeEntry();
                        }
                    }
                    // Otorgar permisos de ejecución (chmod +x) al binario extraído
                    tunnelBin.setExecutable(true);
                }

                // Iniciar el proceso del túnel apuntando al puerto local 8080
                String binPath = tunnelBin.getAbsolutePath();
                ProcessBuilder pb = new ProcessBuilder(binPath, "tunnel", "--url", "http://localhost:8080");
                pb.redirectErrorStream(true);
                
                tunnelProcess = pb.start();
                Log.i(TAG, "🚀 Túnel de Cloudflare iniciado correctamente en segundo plano");

                // Leer la salida del proceso para capturar la URL pública generada
                try (InputStream processInput = tunnelProcess.getInputStream();
                     java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(processInput))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Log.d(TAG, line);
                        if (line.contains("trycloudflare.com")) {
                            String urlPublica = extraerUrlDeLinea(line);
                            if (urlPublica != null) {
                                Log.i(TAG, "🌐 URL Pública detectada: " + urlPublica);
                                // Reportar automáticamente a Firebase
                                DeviceRegistryManager.registrarDispositivoEnNube(context, urlPublica);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error al iniciar o descomprimir el túnel de Cloudflare", e);
            }
        }, "Prux-Tunnel-Thread").start();
    }

    private static String extraerUrlDeLinea(String linea) {
        try {
            int index = linea.indexOf("https://");
            if (index != -1) {
                String sub = linea.substring(index);
                int spaceIndex = sub.indexOf(" ");
                return spaceIndex != -1 ? sub.substring(0, spaceIndex) : sub;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static synchronized void detenerTunel() {
        if (tunnelProcess != null) {
            tunnelProcess.destroy();
            tunnelProcess = null;
            Log.i(TAG, "Túnel de Cloudflare detenido.");
        }
    }
}
