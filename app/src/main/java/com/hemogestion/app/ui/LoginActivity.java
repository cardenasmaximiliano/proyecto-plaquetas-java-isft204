package com.hemogestion.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hemogestion.app.R;
import com.hemogestion.app.api.HemoApi;
import com.hemogestion.app.util.Errores;
import com.hemogestion.app.util.Hilos;

public class LoginActivity extends AppCompatActivity {

    private HemoApi api;
    private TextInputEditText inputUsuario;
    private TextInputEditText inputPassword;
    private MaterialButton botonIngresar;
    private TextView mensaje;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        api = new HemoApi(this);
        if (api.sesion().activa()) {
            irAInicio();
            return;
        }
        setContentView(R.layout.activity_login);
        Bordes.aplicar(this);
        inputUsuario = findViewById(R.id.inputUsuario);
        inputPassword = findViewById(R.id.inputPassword);
        botonIngresar = findViewById(R.id.botonIngresar);
        mensaje = findViewById(R.id.mensaje);
        botonIngresar.setOnClickListener(v -> ingresar());
    }

    private void ingresar() {
        String usuario = textoDe(inputUsuario).trim();
        String password = textoDe(inputPassword);
        if (usuario.isEmpty() || password.isEmpty()) {
            mostrarError(getString(R.string.login_error_campos));
            return;
        }
        botonIngresar.setEnabled(false);
        mensaje.setVisibility(View.GONE);
        Hilos.enFondo(() -> {
            try {
                api.login(usuario, password);
                runOnUiThread(this::irAInicio);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    botonIngresar.setEnabled(true);
                    mostrarError(Errores.mensaje(this, e));
                });
            }
        });
    }

    private void mostrarError(String texto) {
        mensaje.setText(texto);
        mensaje.setVisibility(View.VISIBLE);
    }

    private void irAInicio() {
        Intent intent = new Intent(this, InicioActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }
}
