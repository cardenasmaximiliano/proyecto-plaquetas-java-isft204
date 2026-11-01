package com.hemogestion.app.ui;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Desde Android 15 las pantallas se dibujan de borde a borde (por debajo de la barra de estado y de la de gestos).
 * Esto deja un margen igual al de esas barras y al del teclado, para que nada quede tapado.
 */
public final class Bordes {

    private Bordes() {
    }

    /** Llamar después de setContentView. */
    public static void aplicar(Activity actividad) {
        View raiz = actividad.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (vista, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());
            vista.setPadding(barras.left, barras.top, barras.right, Math.max(barras.bottom, teclado.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(raiz);
    }
}
