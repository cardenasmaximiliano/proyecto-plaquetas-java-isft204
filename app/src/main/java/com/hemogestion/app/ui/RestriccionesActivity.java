package com.hemogestion.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hemogestion.app.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Catálogo de restricciones médicas del centro (por ejemplo "Embarazo", 300 días). Lo ve todo el equipo; lo modifican
 * el director y el superadmin. Al cargar una en un donante se copia su nombre y la veda sugerida.
 */
public class RestriccionesActivity extends BaseActivity {

    private TextView textoFormulario;
    private TextView textoCantidad;
    private TextInputEditText inputNombre;
    private TextInputEditText inputDescripcion;
    private TextInputEditText inputDiasVeda;
    private View capaDiasVeda;
    private CheckBox checkPermanente;
    private MaterialButton botonGuardar;
    private MaterialButton botonCancelarEdicion;
    private LinearLayout listaTipos;

    private long editandoId = 0;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        if (!haySesion()) {
            return;
        }
        setContentView(R.layout.activity_restricciones);
        textoFormulario = findViewById(R.id.textoFormulario);
        textoCantidad = findViewById(R.id.textoCantidad);
        inputNombre = findViewById(R.id.inputNombre);
        inputDescripcion = findViewById(R.id.inputDescripcion);
        inputDiasVeda = findViewById(R.id.inputDiasVeda);
        capaDiasVeda = findViewById(R.id.capaDiasVeda);
        checkPermanente = findViewById(R.id.checkPermanente);
        botonGuardar = findViewById(R.id.botonGuardarTipo);
        botonCancelarEdicion = findViewById(R.id.botonCancelarEdicion);
        listaTipos = findViewById(R.id.listaTipos);

        findViewById(R.id.botonVolver).setOnClickListener(v -> finish());
        findViewById(R.id.bloqueFormulario).setVisibility(permisos.gestionaRestricciones() ? View.VISIBLE : View.GONE);
        checkPermanente.setOnCheckedChangeListener((vista, marcado) -> capaDiasVeda.setVisibility(marcado ? View.GONE : View.VISIBLE));
        botonGuardar.setOnClickListener(v -> guardar());
        botonCancelarEdicion.setOnClickListener(v -> limpiarFormulario());
        limpiarFormulario();
        recargar();
    }

    private void limpiarFormulario() {
        editandoId = 0;
        inputNombre.setText("");
        inputDescripcion.setText("");
        inputDiasVeda.setText("");
        checkPermanente.setChecked(false);
        textoFormulario.setText(R.string.restricciones_nueva);
        botonCancelarEdicion.setVisibility(View.GONE);
    }

    private void recargar() {
        cargar(() -> api.lista("/configuracion/tipos-restriccion"), this::mostrar);
    }

    private void mostrar(JSONArray tipos) {
        listaTipos.removeAllViews();
        textoCantidad.setText(getString(R.string.restricciones_cantidad, tipos.length()));
        for (int i = 0; i < tipos.length(); i++) {
            final JSONObject tipo = tipos.optJSONObject(i);
            if (tipo == null) {
                continue;
            }
            View tarjeta = getLayoutInflater().inflate(R.layout.item_tipo_restriccion, listaTipos, false);
            ((TextView) tarjeta.findViewById(R.id.textoNombreTipo)).setText(tipo.optString("nombre"));
            boolean permanente = tipo.optBoolean("permanente");
            ((TextView) tarjeta.findViewById(R.id.textoVedaTipo)).setText(permanente ? getString(R.string.restriccion_permanente)
                    : getString(R.string.restricciones_veda_sugerida, tipo.optInt("dias_veda")));
            TextView descripcion = tarjeta.findViewById(R.id.textoDescripcionTipo);
            String texto = tipo.isNull("descripcion") ? "" : tipo.optString("descripcion");
            descripcion.setText(texto);
            descripcion.setVisibility(texto.isEmpty() ? View.GONE : View.VISIBLE);
            View eliminar = tarjeta.findViewById(R.id.botonEliminarTipo);
            if (permisos.gestionaRestricciones()) {
                tarjeta.setOnClickListener(v -> editar(tipo));
                eliminar.setOnClickListener(v -> confirmarEliminar(tipo));
            } else {
                eliminar.setVisibility(View.GONE);
            }
            listaTipos.addView(tarjeta);
        }
    }

    private void editar(JSONObject tipo) {
        editandoId = tipo.optLong("id");
        inputNombre.setText(tipo.optString("nombre"));
        inputDescripcion.setText(tipo.isNull("descripcion") ? "" : tipo.optString("descripcion"));
        checkPermanente.setChecked(tipo.optBoolean("permanente"));
        inputDiasVeda.setText(tipo.isNull("dias_veda") ? "" : String.valueOf(tipo.optInt("dias_veda")));
        textoFormulario.setText(R.string.restricciones_editar);
        botonCancelarEdicion.setVisibility(View.VISIBLE);
    }

    private void guardar() {
        String nombre = texto(inputNombre);
        if (nombre.isEmpty()) {
            inputNombre.setError(getString(R.string.restricciones_falta_nombre));
            return;
        }
        boolean permanente = checkPermanente.isChecked();
        Integer dias = null;
        if (!permanente) {
            try {
                dias = Integer.parseInt(texto(inputDiasVeda));
            } catch (NumberFormatException e) {
                dias = null;
            }
            if (dias == null || dias < 1 || dias > 3650) {
                inputDiasVeda.setError(getString(R.string.restricciones_dias_invalidos));
                return;
            }
        }
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("nombre", nombre);
            cuerpo.put("descripcion", texto(inputDescripcion).isEmpty() ? JSONObject.NULL : texto(inputDescripcion));
            cuerpo.put("permanente", permanente);
            cuerpo.put("dias_veda", dias == null ? JSONObject.NULL : dias);
        } catch (JSONException e) {
            mostrarError(e.getMessage());
            return;
        }
        final boolean nuevo = editandoId == 0;
        final String ruta = nuevo ? "/configuracion/tipos-restriccion" : "/configuracion/tipos-restriccion/" + editandoId;
        botonGuardar.setEnabled(false);
        cargar(() -> api.objeto(nuevo ? "POST" : "PUT", ruta, cuerpo), r -> {
            botonGuardar.setEnabled(true);
            aviso(getString(nuevo ? R.string.restricciones_creada : R.string.restricciones_actualizada));
            limpiarFormulario();
            recargar();
        }, () -> botonGuardar.setEnabled(true));
    }

    private void confirmarEliminar(JSONObject tipo) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.restricciones_eliminar_confirmar, tipo.optString("nombre")))
                .setPositiveButton(R.string.restricciones_eliminar, (d, w) -> {
                    final long id = tipo.optLong("id");
                    cargar(() -> {
                        api.borrar("/configuracion/tipos-restriccion/" + id);
                        return Boolean.TRUE;
                    }, r -> {
                        aviso(getString(R.string.restricciones_eliminada));
                        if (editandoId == id) {
                            limpiarFormulario();
                        }
                        recargar();
                    });
                })
                .setNegativeButton(R.string.volver, null)
                .show();
    }
}
