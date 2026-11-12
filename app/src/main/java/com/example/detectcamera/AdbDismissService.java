package com.example.detectcamera;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class AdbDismissService extends AccessibilityService {

    private static final String TAG = "PRUX_ACCESSIBILITY";

    private static final String[] TARGET_TEXT = {
        "Se conectó la depuración inalámbrica",
        "Se conecto la depuracion inalambrica",
        "Depuración inalámbrica",
        "Depuracion inalambrica",
        "Wireless debugging"
    };

    private long lastActionTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        // Limitar frecuencia (debounce de 2 segundos para evitar bucles)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime < 2000) {
            return;
        }

        CharSequence packageName = event.getPackageName();
        if (packageName == null || !"com.android.systemui".equals(packageName.toString())) {
            return;
        }

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        try {
            // PASO 1: Intentar buscar y hacer clic directamente en la opción de "2 horas" si el menú ya está abierto
            if (buscarYHacerClicTexto(rootNode, "2 horas") || buscarYHacerClicTexto(rootNode, "120 min")) {
                lastActionTime = currentTime;
                Log.i(TAG, "⏱️ Opción de 2 horas seleccionada automáticamente");
                return;
            }

            // PASO 2: Si el menú de 2 horas no está visible, verificar si está la notificación base
            boolean encontrada = false;
            for (String text : TARGET_TEXT) {
                List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
                if (nodes != null && !nodes.isEmpty()) {
                    encontrada = true;
                    for (AccessibilityNodeInfo node : nodes) {
                        node.recycle();
                    }
                    break;
                }
            }

            if (encontrada) {
                Log.d(TAG, "🎯 Notificación de depuración detectada. Buscando botón 'Posponer'...");
                // Intentar presionar el botón "Posponer" para desplegar las opciones de tiempo
                if (buscarYHacerClicTexto(rootNode, "Posponer") || buscarYHacerClicTexto(rootNode, "posponer")) {
                    lastActionTime = currentTime;
                    Log.i(TAG, "📌 Botón 'Posponer' presionado para desplegar opciones");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error procesando evento de accesibilidad", e);
        } finally {
            try {
                rootNode.recycle();
            } catch (Exception ignored) {}
        }
    }

    private boolean buscarYHacerClicTexto(AccessibilityNodeInfo root, String textoObjetivo) {
        if (root == null) return false;

        try {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(textoObjetivo);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node != null) {
                        // Intentar hacer clic en el nodo o en sus padres interactivos
                        if (hacerClicEnNodoOPadre(node)) {
                            node.recycle();
                            // Limpiar la lista restante
                            for (AccessibilityNodeInfo n : nodes) {
                                if (n != null) n.recycle();
                            }
                            return true;
                        }
                        node.recycle();
                    }
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    private boolean hacerClicEnNodoOPadre(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null) {
            try {
                if (current.isClickable()) {
                    boolean success = current.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    if (success) return true;
                }
            } catch (Exception ignored) {}

            AccessibilityNodeInfo parent = null;
            try {
                parent = current.getParent();
            } catch (Exception ignored) {}

            current = parent;
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "AdbDismissService interrumpido");
    }
}
