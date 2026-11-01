package com.hemogestion.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.hemogestion.app.R;
import com.hemogestion.app.api.HemoApi;
import com.hemogestion.app.modelo.Permisos;

/**
 * Usuarios del sistema: "Nuevo usuario" (SuperAdmin y Director) y "Ver usuarios" (solo SuperAdmin).
 */
public class UsuariosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.activity_usuarios);

        Permisos permisos = new Permisos(new HemoApi(this).sesion().rol());
        if (!permisos.creaUsuarios()) {
            finish();
            return;
        }
        findViewById(R.id.botonVolverUsuarios).setOnClickListener(v -> finish());
        findViewById(R.id.tileNuevoUsuario).setOnClickListener(v ->
                startActivity(new Intent(this, CrearUsuarioActivity.class)));

        View tileVer = findViewById(R.id.tileVerUsuarios);
        tileVer.setVisibility(permisos.veUsuarios() ? View.VISIBLE : View.GONE);
        tileVer.setOnClickListener(v -> startActivity(new Intent(this, ListaUsuariosActivity.class)));
    }
}
