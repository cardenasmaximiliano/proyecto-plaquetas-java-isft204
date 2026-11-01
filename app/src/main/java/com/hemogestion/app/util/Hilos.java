package com.hemogestion.app.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Hilo de fondo para las llamadas a la API. Los resultados se vuelven a la interfaz con runOnUiThread. */
public final class Hilos {

    private static final ExecutorService FONDO = Executors.newFixedThreadPool(2);

    private Hilos() {
    }

    public static void enFondo(Runnable tarea) {
        FONDO.execute(tarea);
    }
}
