package com.hemogestion.app.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hemogestion.app.R;
import com.hemogestion.app.api.HemoApi;
import com.hemogestion.app.modelo.Permisos;
import com.hemogestion.app.modelo.Usuario;
import com.hemogestion.app.util.Errores;
import com.hemogestion.app.util.Hilos;

import org.json.JSONArray;
import org.json.JSONObject;

/** Usuarios del sistema con cambio de contraseña y activar/desactivar. Solo el SuperAdmin llega acá. */
public class ListaUsuariosActivity extends AppCompatActivity {

    private HemoApi api;
    private LinearLayout lista;
    private TextView textoVacio;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.activity_lista_usuarios);
        api = new HemoApi(this);
        if (!new Permisos(api.sesion().rol()).veUsuarios()) {
            finish();
            return;
        }
        lista = findViewById(R.id.listaUsuarios);
        textoVacio = findViewById(R.id.textoSinUsuarios);
        findViewById(R.id.botonVolverLista).setOnClickListener(v -> finish());
        cargar();
    }

    private void cargar() {
        Hilos.enFondo(() -> {
            try {
                JSONArray usuarios = api.lista("/usuarios");
                runOnUiThread(() -> mostrar(usuarios));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!Errores.volverALoginSiVencida(this, e)) {
                        textoVacio.setText(Errores.mensaje(this, e));
                        textoVacio.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void mostrar(JSONArray usuarios) {
        lista.removeAllViews();
        textoVacio.setVisibility(usuarios.length() == 0 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < usuarios.length(); i++) {
            JSONObject u = usuarios.optJSONObject(i);
            if (u == null) {
                continue;
            }
            final long id = u.optLong("id");
            final String nombre = u.optString("nombre") + " " + u.optString("apellido");
            final boolean activo = u.optBoolean("activo");

            View fila = getLayoutInflater().inflate(R.layout.item_usuario, lista, false);
            ((TextView) fila.findViewById(R.id.textoNombreUsuario)).setText(nombre);
            ((TextView) fila.findViewById(R.id.textoDetalleUsuario)).setText(
                    u.optString("usuario") + " · " + Usuario.nombreDelRol(u.optString("rol")) + " · "
                            + getString(activo ? R.string.usuarios_estado_activo : R.string.usuarios_inactivo));
            fila.findViewById(R.id.botonClaveUsuario).setOnClickListener(v -> pedirClave(id, nombre));
            MaterialButton botonEstado = fila.findViewById(R.id.botonEstadoUsuario);
            botonEstado.setText(activo ? R.string.usuarios_desactivar : R.string.usuarios_activar);
            botonEstado.setOnClickListener(v -> confirmarEstado(id, nombre, !activo));
            lista.addView(fila);
        }
    }

    private void pedirClave(long id, String nombre) {
        EditText campo = new EditText(this);
        campo.setHint(R.string.usuarios_clave_nueva);
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int margen = Math.round(20 * getResources().getDisplayMetrics().density);
        campo.setPadding(margen, margen, margen, margen);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.usuarios_clave_titulo, nombre))
                .setMessage(R.string.usuarios_clave_ayuda)
                .setView(campo)
                .setPositiveButton(R.string.usuarios_clave, (dialogo, w) -> guardarClave(id, campo.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void guardarClave(long id, String clave) {
        if (clave.length() < 8) {
            Toast.makeText(this, R.string.usuario_error_password, Toast.LENGTH_LONG).show();
            return;
        }
        Hilos.enFondo(() -> {
            try {
                api.objeto("POST", "/usuarios/" + id + "/clave", new JSONObject().put("password", clave));
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.usuarios_clave_cambiada, Toast.LENGTH_LONG).show();
                    cargar();
                });
            } catch (Exception e) {
                runOnUiThread(() -> mostrarErrorAccion(e));
            }
        });
    }

    private void confirmarEstado(long id, String nombre, boolean activar) {
        new AlertDialog.Builder(this)
                .setMessage(getString(activar ? R.string.usuarios_confirmar_activar : R.string.usuarios_confirmar_desactivar, nombre))
                .setPositiveButton(activar ? R.string.usuarios_activar : R.string.usuarios_desactivar,
                        (dialogo, w) -> cambiarEstado(id, activar))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void cambiarEstado(long id, boolean activar) {
        Hilos.enFondo(() -> {
            try {
                api.objeto("POST", "/usuarios/" + id + "/activo", new JSONObject().put("activo", activar));
                runOnUiThread(() -> {
                    Toast.makeText(this, activar ? R.string.usuarios_activado : R.string.usuarios_desactivado, Toast.LENGTH_SHORT).show();
                    cargar();
                });
            } catch (Exception e) {
                runOnUiThread(() -> mostrarErrorAccion(e));
            }
        });
    }

    private void mostrarErrorAccion(Exception e) {
        if (!Errores.volverALoginSiVencida(this, e)) {
            Toast.makeText(this, Errores.mensaje(this, e), Toast.LENGTH_LONG).show();
        }
    }
}
