package com.hemogestion.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hemogestion.app.api.HemoApi;
import com.hemogestion.app.modelo.Permisos;
import com.hemogestion.app.util.Errores;
import com.hemogestion.app.util.Hilos;

/**
 * Base de las pantallas que usan la API: exige sesión iniciada y resuelve el patrón de siempre
 * (llamar a la API en un hilo de fondo, volver a la pantalla, mostrar el error o volver al ingreso).
 */
public abstract class BaseActivity extends AppCompatActivity {

    /** Trabajo que se hace en segundo plano (habla con la API). */
    protected interface Trabajo<T> {
        T hacer() throws Exception;
    }

    /** Lo que se hace en pantalla cuando el trabajo terminó bien. */
    protected interface Exito<T> {
        void con(T valor);
    }

    protected HemoApi api;
    protected Permisos permisos;

    @Override
    protected void onCreate(@Nullable Bundle estado) {
        super.onCreate(estado);
        api = new HemoApi(this);
        permisos = new Permisos(api.sesion().rol());
        if (!api.sesion().activa()) {
            irALogin();
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        Bordes.aplicar(this);
    }

    /** Las pantallas llaman a esto antes de armar la interfaz: false si no hay sesión y ya se fue al ingreso. */
    protected boolean haySesion() {
        return api.sesion().activa();
    }

    protected <T> void cargar(Trabajo<T> trabajo, Exito<T> exito) {
        cargar(trabajo, exito, null);
    }

    protected <T> void cargar(Trabajo<T> trabajo, Exito<T> exito, @Nullable Runnable alFallar) {
        Hilos.enFondo(() -> {
            try {
                T resultado = trabajo.hacer();
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        exito.con(resultado);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (alFallar != null) {
                        alFallar.run();
                    }
                    if (!Errores.volverALoginSiVencida(this, e)) {
                        mostrarError(Errores.mensaje(this, e));
                    }
                });
            }
        });
    }

    protected void mostrarError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    protected void aviso(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    protected void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    protected static String texto(com.google.android.material.textfield.TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
