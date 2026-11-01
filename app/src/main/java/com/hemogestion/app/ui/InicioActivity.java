package com.hemogestion.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hemogestion.app.R;
import com.hemogestion.app.api.HemoApi;
import com.hemogestion.app.modelo.Permisos;
import com.hemogestion.app.modelo.Usuario;
import com.hemogestion.app.util.Hilos;

public class InicioActivity extends AppCompatActivity {

    private HemoApi api;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        api = new HemoApi(this);
        if (!api.sesion().activa()) {
            irALogin();
            return;
        }
        setContentView(R.layout.activity_inicio);
        Bordes.aplicar(this);

        ((TextView) findViewById(R.id.textoSaludo)).setText(getString(R.string.inicio_saludo, api.sesion().nombre()));
        ((TextView) findViewById(R.id.textoRol)).setText(Usuario.nombreDelRol(api.sesion().rol()));

        findViewById(R.id.moduloDonantes).setOnClickListener(v -> startActivity(new Intent(this, DonantesActivity.class)));
        findViewById(R.id.campoBuscar).setOnClickListener(v -> startActivity(new Intent(this, DonantesActivity.class)));
        findViewById(R.id.moduloAgenda).setOnClickListener(v -> startActivity(new Intent(this, AgendaActivity.class)));
        findViewById(R.id.moduloRestricciones).setOnClickListener(v -> startActivity(new Intent(this, RestriccionesActivity.class)));
        findViewById(R.id.moduloBusqueda).setOnClickListener(v -> startActivity(new Intent(this, BusquedaAvanzadaActivity.class)));
        findViewById(R.id.moduloEstadisticas).setOnClickListener(v -> startActivity(new Intent(this, EstadisticasActivity.class)));
        View moduloUsuarios = findViewById(R.id.moduloUsuarios);
        moduloUsuarios.setVisibility(new Permisos(api.sesion().rol()).creaUsuarios() ? View.VISIBLE : View.GONE);
        moduloUsuarios.setOnClickListener(v -> startActivity(new Intent(this, UsuariosActivity.class)));
        BarraInferior.configurar(this, BarraInferior.INICIO);

        findViewById(R.id.botonSalir).setOnClickListener(v -> Hilos.enFondo(() -> {
            api.cerrarSesion();
            runOnUiThread(this::irALogin);
        }));
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
