package com.hemogestion.app.api;

import android.content.Context;
import android.content.SharedPreferences;


public final class Sesion {

    private static final String ARCHIVO = "hemo_sesion";
    private static final String TOKEN = "token";
    private static final String NOMBRE = "nombre";
    private static final String ROL = "rol";

    private final SharedPreferences prefs;

    public Sesion(Context contexto) {
        prefs = contexto.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    public void guardar(String token, String nombre, String rol) {
        prefs.edit().putString(TOKEN, token).putString(NOMBRE, nombre).putString(ROL, rol).apply();
    }

    public String token() {
        return prefs.getString(TOKEN, null);
    }

    public String nombre() {
        return prefs.getString(NOMBRE, "");
    }

    /** Código del rol tal como lo devuelve la API (SUPERADMIN, DIRECTOR, EQUIPO_TRABAJO). */
    public String rol() {
        return prefs.getString(ROL, "");
    }

    public boolean activa() {
        return token() != null;
    }

    public void cerrar() {
        prefs.edit().clear().apply();
    }
}
