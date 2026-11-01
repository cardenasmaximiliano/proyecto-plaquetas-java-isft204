package com.hemogestion.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hemogestion.app.R;
import com.hemogestion.app.api.HemoApi;
import com.hemogestion.app.modelo.Permisos;
import com.hemogestion.app.modelo.Usuario;
import com.hemogestion.app.util.Errores;
import com.hemogestion.app.util.Hilos;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Alta de un usuario del sistema. El SuperAdmin puede crear cualquier rol; el Director, solo Director o
 * Equipo de trabajo. La API vuelve a verificar ambas reglas.
 */
public class CrearUsuarioActivity extends AppCompatActivity {

    private static final String[] TODOS_LOS_ROLES = {"SUPERADMIN", "DIRECTOR", "EQUIPO_TRABAJO"};

    private HemoApi api;
    private String[] rolesPermitidos;
    private TextInputEditText inputNombre;
    private TextInputEditText inputApellido;
    private TextInputEditText inputUsuario;
    private TextInputEditText inputPassword;
    private TextInputEditText inputMatricula;
    private Spinner spinnerRol;
    private TextView textoError;
    private MaterialButton botonGuardar;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.activity_crear_usuario);
        api = new HemoApi(this);

        String rol = api.sesion().rol();
        if (!new Permisos(rol).creaUsuarios()) {
            finish();
            return;
        }
        rolesPermitidos = "SUPERADMIN".equals(rol)
                ? TODOS_LOS_ROLES
                : new String[] {"DIRECTOR", "EQUIPO_TRABAJO"};

        inputNombre = findViewById(R.id.inputNombre);
        inputApellido = findViewById(R.id.inputApellido);
        inputUsuario = findViewById(R.id.inputUsuario);
        inputPassword = findViewById(R.id.inputPassword);
        inputMatricula = findViewById(R.id.inputMatricula);
        spinnerRol = findViewById(R.id.spinnerRol);
        textoError = findViewById(R.id.textoError);
        botonGuardar = findViewById(R.id.botonGuardarUsuario);

        String[] nombres = new String[rolesPermitidos.length];
        for (int i = 0; i < rolesPermitidos.length; i++) {
            nombres[i] = Usuario.nombreDelRol(rolesPermitidos[i]);
        }
        spinnerRol.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombres));

        findViewById(R.id.botonVolverUsuario).setOnClickListener(v -> finish());
        botonGuardar.setOnClickListener(v -> guardar());
    }

    private void guardar() {
        String nombre = textoDe(inputNombre);
        String apellido = textoDe(inputApellido);
        String usuario = textoDe(inputUsuario);
        String password = inputPassword.getText() == null ? "" : inputPassword.getText().toString();
        String matricula = textoDe(inputMatricula);

        if (nombre.isEmpty() || apellido.isEmpty() || usuario.isEmpty() || password.isEmpty()) {
            mostrarError(getString(R.string.usuario_error_obligatorios));
            return;
        }
        if (nombre.matches(".*\\d.*") || apellido.matches(".*\\d.*")) {
            mostrarError(getString(R.string.form_error_nombre_numeros));
            return;
        }
        if (password.length() < 8) {
            mostrarError(getString(R.string.usuario_error_password));
            return;
        }

        final JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("nombre", nombre);
            cuerpo.put("apellido", apellido);
            cuerpo.put("usuario", usuario);
            cuerpo.put("password", password);
            cuerpo.put("rol", rolesPermitidos[spinnerRol.getSelectedItemPosition()]);
            if (!matricula.isEmpty()) {
                cuerpo.put("matricula", matricula);
            }
        } catch (JSONException e) {
            mostrarError(Errores.mensaje(this, e));
            return;
        }

        textoError.setVisibility(View.GONE);
        botonGuardar.setEnabled(false);
        Hilos.enFondo(() -> {
            try {
                api.objeto("POST", "/usuarios/crear", cuerpo);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.usuario_creado, Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    botonGuardar.setEnabled(true);
                    if (!Errores.volverALoginSiVencida(this, e)) {
                        mostrarError(Errores.mensaje(this, e));
                    }
                });
            }
        });
    }

    private void mostrarError(String texto) {
        textoError.setText(texto);
        textoError.setVisibility(View.VISIBLE);
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
