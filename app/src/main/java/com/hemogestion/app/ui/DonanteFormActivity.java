package com.hemogestion.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hemogestion.app.R;
import com.hemogestion.app.api.HemoApi;
import com.hemogestion.app.modelo.Donante;
import com.hemogestion.app.util.Errores;
import com.hemogestion.app.util.Hilos;
import com.hemogestion.app.util.Texto;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.hemogestion.app.util.Fechas;

/** Alta o edición de un donante. Sin extra EXTRA_ID crea uno nuevo. */
public class DonanteFormActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "donante_id";

    // Valores que guarda la API para cada opción, en el mismo orden que los textos de strings/arrays.xml.
    private static final String[] SEXO_CODIGOS = {"F", "M"};
    private static final String[] GRUPO_CODIGOS = {"", "A", "B", "AB", "0"};
    private static final String[] FACTOR_CODIGOS = {"", "+", "-"};

    private HemoApi api;
    private Donante donante;   // el que se está editando (o el nuevo que se arma)
    private boolean esNuevo;   // true: alta (POST); false: edición de uno existente (PUT)
    private JSONArray restricciones = new JSONArray();  // las que muestra la lista; se guardan con el donante

    private TextView textoTitulo;
    private TextView textoError;
    private TextInputEditText inputDni;
    private TextInputEditText inputNombre;
    private TextInputEditText inputApellido;
    private TextInputEditText inputNacimiento;
    private TextInputEditText inputPeso;
    private TextInputEditText inputAltura;
    private TextInputEditText inputTelefono;
    private TextInputEditText inputEmail;
    private TextInputEditText inputDomicilio;
    private TextInputEditText inputLocalidad;
    private TextInputEditText inputObservaciones;
    // Informe de laboratorio del donante (opcional); null = no hay informe.
    private JSONObject informe;
    private TextView textoEstadoInforme;
    private ActivityResultLauncher<Intent> lanzadorInforme;
    private Spinner spinnerSexo;
    private Spinner spinnerGrupo;
    private Spinner spinnerFactor;
    private CheckBox checkWhatsapp;
    private CheckBox checkEmail;
    private CheckBox checkDiferido;
    private LinearLayout listaRestricciones;
    private MaterialButton botonAgregarRestriccion;
    private MaterialButton botonGuardar;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        api = new HemoApi(this);
        if (!api.sesion().activa()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_donante_form);
        Bordes.aplicar(this);
        enlazarVistas();
        cargarOpciones();

        findViewById(R.id.botonVolver).setOnClickListener(v -> finish());
        botonAgregarRestriccion.setOnClickListener(v -> pedirRestriccion());
        botonGuardar.setOnClickListener(v -> guardar());

        long id = getIntent().getLongExtra(EXTRA_ID, 0);
        esNuevo = id == 0;
        if (esNuevo) {
            try {
                donante = Donante.nuevo();
            } catch (JSONException e) {
                Toast.makeText(this, Errores.mensaje(this, e), Toast.LENGTH_LONG).show();
            }
            textoTitulo.setText(R.string.form_titulo_nuevo);
            mostrarRestricciones();
        } else {
            textoTitulo.setText(R.string.form_titulo_editar);
            cargarDonante(id);
        }
    }

    private void enlazarVistas() {
        textoTitulo = findViewById(R.id.textoTitulo);
        textoError = findViewById(R.id.textoError);
        inputDni = findViewById(R.id.inputDni);
        inputNombre = findViewById(R.id.inputNombre);
        inputApellido = findViewById(R.id.inputApellido);
        inputNacimiento = findViewById(R.id.inputNacimiento);
        inputPeso = findViewById(R.id.inputPeso);
        inputAltura = findViewById(R.id.inputAltura);
        inputTelefono = findViewById(R.id.inputTelefono);
        inputEmail = findViewById(R.id.inputEmail);
        inputDomicilio = findViewById(R.id.inputDomicilio);
        inputLocalidad = findViewById(R.id.inputLocalidad);
        inputObservaciones = findViewById(R.id.inputObservaciones);
        textoEstadoInforme = findViewById(R.id.textoEstadoInforme);
        lanzadorInforme = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
            if (resultado.getResultCode() != RESULT_OK) {
                return;  // volvió sin aceptar: el informe queda como estaba
            }
            Intent datos = resultado.getData();
            String json = datos == null ? null : datos.getStringExtra(InformeLaboratorioActivity.EXTRA_RESULTADO);
            try {
                informe = json == null ? null : new JSONObject(json);
            } catch (JSONException e) {
                informe = null;
            }
            mostrarEstadoInforme();
        });
        findViewById(R.id.botonInforme).setOnClickListener(v -> abrirInforme());
        mostrarEstadoInforme();
        spinnerSexo = findViewById(R.id.spinnerSexo);
        spinnerGrupo = findViewById(R.id.spinnerGrupo);
        spinnerFactor = findViewById(R.id.spinnerFactor);
        checkWhatsapp = findViewById(R.id.checkWhatsapp);
        checkEmail = findViewById(R.id.checkEmail);
        checkDiferido = findViewById(R.id.checkDiferido);
        listaRestricciones = findViewById(R.id.listaRestricciones);
        botonAgregarRestriccion = findViewById(R.id.botonAgregarRestriccion);
        botonGuardar = findViewById(R.id.botonGuardar);
    }

    private void cargarOpciones() {
        spinnerSexo.setAdapter(ArrayAdapter.createFromResource(this, R.array.sexo_nombres, android.R.layout.simple_spinner_dropdown_item));
        spinnerGrupo.setAdapter(ArrayAdapter.createFromResource(this, R.array.grupo_nombres, android.R.layout.simple_spinner_dropdown_item));
        spinnerFactor.setAdapter(ArrayAdapter.createFromResource(this, R.array.factor_nombres, android.R.layout.simple_spinner_dropdown_item));
    }

    private void cargarDonante(long id) {
        Hilos.enFondo(() -> {
            try {
                Donante cargado = api.obtenerDonante(id);
                runOnUiThread(() -> {
                    donante = cargado;
                    completarFormulario(cargado);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!Errores.volverALoginSiVencida(this, e)) {
                        Toast.makeText(this, Errores.mensaje(this, e), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void completarFormulario(Donante d) {
        inputDni.setText(d.texto("numero_documento"));
        inputNombre.setText(d.texto("nombre"));
        inputApellido.setText(d.texto("apellido"));
        inputNacimiento.setText(d.texto("fecha_nacimiento"));
        inputPeso.setText(d.texto("peso_kg"));
        inputAltura.setText(d.texto("altura_cm"));
        inputTelefono.setText(d.texto("telefono_e164"));
        inputEmail.setText(d.texto("email"));
        inputDomicilio.setText(d.texto("domicilio"));
        inputLocalidad.setText(d.texto("localidad"));
        inputObservaciones.setText(d.texto("observaciones"));
        spinnerSexo.setSelection(indiceDe(SEXO_CODIGOS, d.texto("sexo")));
        spinnerGrupo.setSelection(indiceDe(GRUPO_CODIGOS, d.texto("grupo_sanguineo")));
        spinnerFactor.setSelection(indiceDe(FACTOR_CODIGOS, d.texto("factor_rh")));
        checkWhatsapp.setChecked(d.bool("acepta_whatsapp"));
        checkEmail.setChecked(d.bool("acepta_email"));
        checkDiferido.setChecked(d.bool("diferido_permanente"));
        restricciones = d.restricciones();
        mostrarRestricciones();
        informe = d.objeto("informe_laboratorio");
        mostrarEstadoInforme();
    }

    private void guardar() {
        textoError.setVisibility(View.GONE);
        if (!esNuevo && donante == null) {
            mostrarError(getString(R.string.cargando));  // todavía no terminó de traer al donante
            return;
        }
        String dni = textoDe(inputDni).trim();
        String nombre = textoDe(inputNombre).trim();
        String apellido = textoDe(inputApellido).trim();
        String nacimiento = textoDe(inputNacimiento).trim();
        String peso = textoDe(inputPeso).trim().replace(',', '.');
        String altura = textoDe(inputAltura).trim().replace(',', '.');
        String telefono = textoDe(inputTelefono).trim();
        String email = textoDe(inputEmail).trim();

        if (dni.isEmpty() || nombre.isEmpty() || apellido.isEmpty() || nacimiento.isEmpty() || peso.isEmpty()) {
            mostrarError(getString(R.string.form_error_obligatorios));
            return;
        }
        if (!Texto.esFecha(nacimiento)) {
            mostrarError(getString(R.string.form_error_fecha));
            return;
        }
        if (nombre.matches(".*\\d.*") || apellido.matches(".*\\d.*")) {
            mostrarError(getString(R.string.form_error_nombre_numeros));
            return;
        }
        double pesoKg;
        try {
            pesoKg = Double.parseDouble(peso);
        } catch (NumberFormatException e) {
            mostrarError(getString(R.string.form_error_peso));
            return;
        }
        if (pesoKg <= 0) {
            mostrarError(getString(R.string.form_error_peso));
            return;
        }
        if (!altura.isEmpty()) {
            try {
                if (Double.parseDouble(altura) <= 0) {
                    mostrarError(getString(R.string.form_error_altura));
                    return;
                }
            } catch (NumberFormatException e) {
                mostrarError(getString(R.string.form_error_altura));
                return;
            }
        }
        if (!Texto.esTelefono(telefono)) {
            mostrarError(getString(R.string.form_error_telefono));
            return;
        }
        if (!email.isEmpty() && !email.contains("@")) {
            mostrarError(getString(R.string.form_error_email));
            return;
        }

        final boolean altaNueva = esNuevo;
        final Donante d;
        try {
            if (donante == null) {
                donante = Donante.nuevo();  // la carga del donante todavía no terminó (o falló)
            }
            d = donante;
            d.set("tipo_documento", "DNI");
            d.set("numero_documento", dni);
            d.set("nombre", nombre);
            d.set("apellido", apellido);
            d.set("busqueda_normalizada", Texto.normalizar(apellido + " " + nombre));
            d.set("fecha_nacimiento", nacimiento);
            d.set("sexo", SEXO_CODIGOS[spinnerSexo.getSelectedItemPosition()]);
            d.set("peso_kg", pesoKg);
            d.set("altura_cm", altura.isEmpty() ? JSONObject.NULL : Double.parseDouble(altura));
            d.set("grupo_sanguineo", vacioANull(GRUPO_CODIGOS[spinnerGrupo.getSelectedItemPosition()]));
            d.set("factor_rh", vacioANull(FACTOR_CODIGOS[spinnerFactor.getSelectedItemPosition()]));
            d.set("telefono_e164", vacioANull(telefono));
            d.set("email", vacioANull(email));
            d.set("domicilio", vacioANull(textoDe(inputDomicilio).trim()));
            d.set("localidad", vacioANull(textoDe(inputLocalidad).trim()));
            d.set("observaciones", vacioANull(textoDe(inputObservaciones).trim()));
            d.set("diferido_permanente", checkDiferido.isChecked());

            boolean whatsapp = checkWhatsapp.isChecked();
            boolean correo = checkEmail.isChecked();
            // La fecha de consentimiento se registra la primera vez que el donante acepta algo.
            if ((whatsapp || correo) && d.texto("fecha_consentimiento").isEmpty()) {
                d.set("fecha_consentimiento", Texto.ahoraIso());
            }
            d.set("acepta_whatsapp", whatsapp);
            d.set("acepta_email", correo);
            d.set("restricciones", restricciones);
            d.set("informe_laboratorio", informe == null ? JSONObject.NULL : informe);
        } catch (JSONException e) {
            mostrarError(Errores.mensaje(this, e));
            return;
        }

        botonGuardar.setEnabled(false);
        Hilos.enFondo(() -> {
            try {
                if (altaNueva) {
                    api.crearDonante(d);
                } else {
                    api.actualizarDonante(d);
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.form_guardado, Toast.LENGTH_SHORT).show();
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

    // ---- Restricciones: la misma lista y el mismo diálogo que en la ficha ---------------------------------

    /** Trae el catálogo del centro y abre el diálogo para agregar una restricción a la lista. */
    private void pedirRestriccion() {
        Hilos.enFondo(() -> {
            try {
                JSONArray catalogo = api.lista("/configuracion/tipos-restriccion");
                runOnUiThread(() -> DialogoRestriccion.mostrar(this, DialogoRestriccion.opciones(this, catalogo), restricciones, r -> {
                    restricciones.put(r);
                    mostrarRestricciones();
                }));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!Errores.volverALoginSiVencida(this, e)) {
                        mostrarError(Errores.mensaje(this, e));
                    }
                });
            }
        });
    }

    private void mostrarRestricciones() {
        listaRestricciones.removeAllViews();
        if (restricciones.length() == 0) {
            TextView vacio = new TextView(this);
            vacio.setText(R.string.detalle_sin_restricciones);
            vacio.setPadding(0, 8, 0, 8);
            listaRestricciones.addView(vacio);
            return;
        }
        for (int i = 0; i < restricciones.length(); i++) {
            final int posicion = i;
            JSONObject r = restricciones.optJSONObject(i);
            if (r == null) {
                continue;
            }
            View fila = getLayoutInflater().inflate(R.layout.item_restriccion, listaRestricciones, false);
            ((TextView) fila.findViewById(R.id.textoRestriccion)).setText(Donante.describir(r));
            fila.findViewById(R.id.botonQuitar).setOnClickListener(v -> {
                restricciones.remove(posicion);
                mostrarRestricciones();
            });
            listaRestricciones.addView(fila);
        }
    }

    // ---- Informe de laboratorio que trae el paciente ---------------------------------------------------------

    /** Abre la pantalla del informe con los valores actuales; al aceptar vuelve con el resultado. */
    private void abrirInforme() {
        Intent intent = new Intent(this, InformeLaboratorioActivity.class);
        intent.putExtra(InformeLaboratorioActivity.EXTRA_INFORME, informe == null ? null : informe.toString());
        lanzadorInforme.launch(intent);
    }

    private void mostrarEstadoInforme() {
        if (informe == null) {
            textoEstadoInforme.setText(R.string.informe_estado_vacio);
            return;
        }
        String fecha = informe.optString("fecha_informe", "");
        textoEstadoInforme.setText(fecha.isEmpty()
                ? getString(R.string.informe_estado_cargado_sin_fecha)
                : getString(R.string.informe_estado_cargado, Fechas.visible(fecha)));
    }

    private void mostrarError(String texto) {
        textoError.setText(texto);
        textoError.setVisibility(View.VISIBLE);
    }

    private static int indiceDe(String[] codigos, String valor) {
        for (int i = 0; i < codigos.length; i++) {
            if (codigos[i].equals(valor)) {
                return i;
            }
        }
        return 0;
    }

    private static Object vacioANull(String valor) {
        return valor.isEmpty() ? JSONObject.NULL : valor;
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }
}
