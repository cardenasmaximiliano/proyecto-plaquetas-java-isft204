package com.hemogestion.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.hemogestion.app.R;
import com.hemogestion.app.api.ApiException;
import com.hemogestion.app.ui.LoginActivity;

/** Traduce los errores a mensajes para el usuario. */
public final class Errores {

    private Errores() {
    }

    public static String mensaje(Context contexto, Exception e) {
        if (e instanceof ApiException) {
            ApiException api = (ApiException) e;
            if (ApiException.SIN_CONEXION.equals(api.getCodigo())) {
                return contexto.getString(R.string.error_sin_conexion);
            }
            if (ApiException.SESION_VENCIDA.equals(api.getCodigo())) {
                return contexto.getString(R.string.error_sesion);
            }
            return api.getMessage();
        }
        return contexto.getString(R.string.error_generico, String.valueOf(e.getMessage()));
    }

    /** Si el token venció, lleva al ingreso y devuelve true. */
    public static boolean volverALoginSiVencida(Activity actividad, Exception e) {
        if (e instanceof ApiException && ApiException.SESION_VENCIDA.equals(((ApiException) e).getCodigo())) {
            Intent intent = new Intent(actividad, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            actividad.startActivity(intent);
            actividad.finish();
            return true;
        }
        return false;
    }
}
