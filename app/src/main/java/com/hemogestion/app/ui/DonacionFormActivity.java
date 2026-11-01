package com.hemogestion.app.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hemogestion.app.R;
import com.hemogestion.app.api.HemoApi;
import com.hemogestion.app.modelo.TipoDonacion;
import com.hemogestion.app.util.Fechas;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/**
 * Registra una donación. El equipo de trabajo la carga en estado "Pendiente"; el resultado (apto o diferido) lo define
 * el director o el superadmin. La API revisa las reglas y devuelve avisos (por ejemplo, si no estaba habilitado).
 */
public class DonacionFormActivity extends BaseActivity {

    public static final String EXTRA_DONANTE_ID = "donante_id";
    public static final String EXTRA_DONANTE_NOMBRE = "donante_nombre";
    public static final String EXTRA_TURNO_ID = "turno_id";
    public static final String EXTRA_TIPO = "tipo";
    /** Si viene (la donación como JSON de la API), la pantalla corrige esa donación en lugar de registrar una nueva. */
    public static final String EXTRA_DONACION = "donacion";

    private static final String[] RESULTADOS = {"PENDIENTE", "APTO", "DIFERIDO_TEMPORAL", "DIFERIDO_PERMANENTE"};

    private long donanteId;
    private long turnoId;
    private String fecha;
    private String hora;
    private String diferidoHasta;
    private List<TipoDonacion> tipos;
    private String tipoPedido;
    private JSONObject donacionEditada;   // null al registrar una donación nueva

    private TextView textoDonante;
    private TextView textoElegibilidad;
    private Spinner spinnerTipo;
    private Spinner spinnerResultado;
    private MaterialButton botonFecha;
    private MaterialButton botonHora;
    private MaterialButton botonDiferidoHasta;
    private MaterialButton botonGuardar;
    private View capaMotivo;
    private CheckBox checkSeCompleto;
    private TextInputEditText inputPeso;
    private TextInputEditText inputTemperatura;
    private TextInputEditText inputSistolica;
    private TextInputEditText inputDiastolica;
    private TextInputEditText inputPulso;
    private TextInputEditText inputHemoglobina;
    private TextInputEditText inputHematocrito;
    private TextInputEditText inputPlaquetas;
    private TextInputEditText inputVolumen;
    private TextInputEditText inputMotivo;
    private TextInputEditText inputObservaciones;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        if (!haySesion()) {
            return;
        }
        setContentView(R.layout.activity_donacion_form);
        enlazar();
        donanteId = getIntent().getLongExtra(EXTRA_DONANTE_ID, 0);
        turnoId = getIntent().getLongExtra(EXTRA_TURNO_ID, 0);
        tipoPedido = getIntent().getStringExtra(EXTRA_TIPO);
        fecha = Fechas.hoy();
        hora = Fechas.horaActual();
        String json = getIntent().getStringExtra(EXTRA_DONACION);
        if (json != null) {
            try {
                donacionEditada = new JSONObject(json);
            } catch (JSONException e) {
                finish();
                return;
            }
            tipoPedido = donacionEditada.optString("tipo_donacion");
            fecha = Fechas.dia(donacionEditada.optString("fecha_hora"));
            hora = Fechas.hora(donacionEditada.optString("fecha_hora"));
            ((TextView) findViewById(R.id.textoTitulo)).setText(R.string.donacion_titulo_corregir);
            botonGuardar.setText(R.string.donacion_guardar_cambios);
        }
        mostrarMomento();

        String nombre = getIntent().getStringExtra(EXTRA_DONANTE_NOMBRE);
        textoDonante.setText(nombre == null ? "" : nombre);
        if (nombre == null && donanteId != 0) {
            cargar(() -> api.obtenerDonante(donanteId), d -> textoDonante.setText(d.nombreCompleto()));
        }

        findViewById(R.id.botonVolver).setOnClickListener(v -> finish());
        botonFecha.setOnClickListener(v -> elegirFecha());
        botonHora.setOnClickListener(v -> elegirHora());
        botonGuardar.setOnClickListener(v -> guardar());
        botonDiferidoHasta.setOnClickListener(v -> elegirDiferidoHasta());

        if (permisos.defineResultado()) {
            findViewById(R.id.bloqueResultado).setVisibility(View.VISIBLE);
            spinnerResultado.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                    getResources().getStringArray(R.array.resultado_nombres)));
            spinnerResultado.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> padre, View vista, int posicion, long id) {
                    actualizarResultado();
                }

                @Override
                public void onNothingSelected(AdapterView<?> padre) {
                }
            });
        }
        if (donacionEditada != null) {
            completarEdicion();
        }
        spinnerTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> padre, View vista, int posicion, long id) {
                consultarElegibilidad();
            }

            @Override
            public void onNothingSelected(AdapterView<?> padre) {
            }
        });
        cargar(() -> api.tiposDonacion(), lista -> {
            tipos = lista;
            spinnerTipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, lista));
            for (int i = 0; i < lista.size(); i++) {
                if (lista.get(i).getCodigo().equals(tipoPedido)) {
                    spinnerTipo.setSelection(i);
                }
            }
        });
    }

    @Override
    protected void mostrarError(String mensaje) {
        new AlertDialog.Builder(this).setMessage(mensaje).setPositiveButton(android.R.string.ok, null).show();
    }

    /** Pone en el formulario los valores actuales de la donación que se corrige. */
    private void completarEdicion() {
        JSONObject d = donacionEditada;
        rellenar(inputPeso, d, "peso_dia_kg");
        rellenar(inputTemperatura, d, "temperatura_c");
        rellenar(inputSistolica, d, "pa_antes_sistolica");
        rellenar(inputDiastolica, d, "pa_antes_diastolica");
        rellenar(inputPulso, d, "frecuencia_cardiaca");
        rellenar(inputPlaquetas, d, "recuento_plaquetas");
        rellenar(inputVolumen, d, "volumen_ml");
        JSONObject h = d.optJSONObject("hemograma");
        if (h != null) {
            rellenar(inputHemoglobina, h, "hemoglobina");
            rellenar(inputHematocrito, h, "hematocrito");
        }
        checkSeCompleto.setChecked(d.optBoolean("se_completo", true));
        if (!d.isNull("observaciones")) {
            inputObservaciones.setText(d.optString("observaciones"));
        }
        if (permisos.defineResultado()) {
            String resultado = d.optString("resultado", "PENDIENTE");
            for (int i = 0; i < RESULTADOS.length; i++) {
                if (RESULTADOS[i].equals(resultado)) {
                    spinnerResultado.setSelection(i);
                }
            }
            if (!d.isNull("motivo_diferimiento")) {
                inputMotivo.setText(d.optString("motivo_diferimiento"));
            }
            diferidoHasta = d.isNull("diferido_hasta") ? null : d.optString("diferido_hasta");
            actualizarResultado();
        }
    }

    private static void rellenar(TextInputEditText campo, JSONObject json, String clave) {
        if (!json.isNull(clave) && json.has(clave)) {
            String valor = String.valueOf(json.opt(clave));
            campo.setText(valor.endsWith(".0") ? valor.substring(0, valor.length() - 2) : valor);
        }
    }

    private void enlazar() {
        textoDonante = findViewById(R.id.textoDonante);
        textoElegibilidad = findViewById(R.id.textoElegibilidad);
        spinnerTipo = findViewById(R.id.spinnerTipo);
        spinnerResultado = findViewById(R.id.spinnerResultado);
        botonFecha = findViewById(R.id.botonFecha);
        botonHora = findViewById(R.id.botonHora);
        botonDiferidoHasta = findViewById(R.id.botonDiferidoHasta);
        botonGuardar = findViewById(R.id.botonGuardarDonacion);
        capaMotivo = findViewById(R.id.capaMotivo);
        checkSeCompleto = findViewById(R.id.checkSeCompleto);
        inputPeso = findViewById(R.id.inputPeso);
        inputTemperatura = findViewById(R.id.inputTemperatura);
        inputSistolica = findViewById(R.id.inputSistolica);
        inputDiastolica = findViewById(R.id.inputDiastolica);
        inputPulso = findViewById(R.id.inputPulso);
        inputHemoglobina = findViewById(R.id.inputHemoglobina);
        inputHematocrito = findViewById(R.id.inputHematocrito);
        inputPlaquetas = findViewById(R.id.inputPlaquetas);
        inputVolumen = findViewById(R.id.inputVolumen);
        inputMotivo = findViewById(R.id.inputMotivo);
        inputObservaciones = findViewById(R.id.inputObservaciones);
    }

    private void mostrarMomento() {
        botonFecha.setText(getString(R.string.donacion_fecha, Fechas.visible(fecha)));
        botonHora.setText(getString(R.string.donacion_hora, hora));
    }

    private void elegirFecha() {
        new DatePickerDialog(this, (vista, anio, mes, dia) -> {
            fecha = Fechas.armar(anio, mes + 1, dia);
            mostrarMomento();
            consultarElegibilidad();
        }, Fechas.anio(fecha), Fechas.mes(fecha) - 1, Fechas.diaDelMes(fecha)).show();
    }

    private void elegirHora() {
        new TimePickerDialog(this, (vista, h, m) -> {
            hora = String.format(Locale.US, "%02d:%02d", h, m);
            mostrarMomento();
        }, Integer.parseInt(hora.substring(0, 2)), Integer.parseInt(hora.substring(3, 5)), true).show();
    }

    private void elegirDiferidoHasta() {
        String base = diferidoHasta != null ? diferidoHasta : Fechas.sumarDias(Fechas.hoy(), 30);
        new DatePickerDialog(this, (vista, anio, mes, dia) -> {
            diferidoHasta = Fechas.armar(anio, mes + 1, dia);
            mostrarDiferidoHasta();
        }, Fechas.anio(base), Fechas.mes(base) - 1, Fechas.diaDelMes(base)).show();
    }

    private void mostrarDiferidoHasta() {
        botonDiferidoHasta.setText(diferidoHasta == null ? getString(R.string.donacion_diferido_hasta_elegir)
                : getString(R.string.donacion_diferido_hasta, Fechas.visible(diferidoHasta)));
    }

    private String resultadoElegido() {
        if (!permisos.defineResultado()) {
            return "PENDIENTE";
        }
        return RESULTADOS[Math.max(0, spinnerResultado.getSelectedItemPosition())];
    }

    private void actualizarResultado() {
        String r = resultadoElegido();
        boolean diferido = r.startsWith("DIFERIDO");
        capaMotivo.setVisibility(diferido ? View.VISIBLE : View.GONE);
        botonDiferidoHasta.setVisibility("DIFERIDO_TEMPORAL".equals(r) ? View.VISIBLE : View.GONE);
        if ("DIFERIDO_TEMPORAL".equals(r)) {
            mostrarDiferidoHasta();
        }
    }

    /** Avisa, antes de guardar, si el donante está habilitado para ese tipo en esa fecha (la API decide). */
    private void consultarElegibilidad() {
        Object elegido = spinnerTipo.getSelectedItem();
        if (donacionEditada != null || donanteId == 0 || !(elegido instanceof TipoDonacion)) {
            return;  // al corregir una donación ya hecha no hace falta (la API la evaluó al registrarla)
        }
        final String codigo = ((TipoDonacion) elegido).getCodigo();
        final String dia = fecha;
        cargar(() -> api.lista("/donantes/" + donanteId + "/elegibilidad?fecha=" + HemoApi.param(dia)), evaluaciones -> {
            if (!dia.equals(fecha)) {
                return;
            }
            for (int i = 0; i < evaluaciones.length(); i++) {
                JSONObject ev = evaluaciones.optJSONObject(i);
                if (ev != null && codigo.equals(ev.optString("tipo"))) {
                    mostrarElegibilidad(ev);
                }
            }
        });
    }

    private void mostrarElegibilidad(JSONObject ev) {
        textoElegibilidad.setVisibility(View.VISIBLE);
        if (ev.optBoolean("apto")) {
            textoElegibilidad.setText(R.string.donacion_habilitado);
            textoElegibilidad.setBackgroundResource(R.drawable.bg_chip_exito);
            textoElegibilidad.setTextColor(ContextCompat.getColor(this, R.color.hemo_exito));
            return;
        }
        StringBuilder motivos = new StringBuilder(getString(R.string.donacion_no_habilitado));
        JSONArray impedimentos = ev.optJSONArray("impedimentos");
        for (int i = 0; impedimentos != null && i < impedimentos.length(); i++) {
            motivos.append("\n• ").append(impedimentos.optJSONObject(i).optString("descripcion"));
        }
        textoElegibilidad.setText(motivos.toString());
        textoElegibilidad.setBackgroundResource(R.drawable.bg_chip_alerta);
        textoElegibilidad.setTextColor(ContextCompat.getColor(this, R.color.hemo_advertencia));
    }

    // ---- Guardar ----------------------------------------------------------------------------------------------

    /** Agrega un número opcional: vacío se omite; inválido es un error. */
    private static void numero(JSONObject cuerpo, String clave, TextInputEditText campo, boolean entero) throws JSONException {
        String t = texto(campo).replace(',', '.');
        if (t.isEmpty()) {
            return;
        }
        try {
            if (entero) {
                cuerpo.put(clave, Integer.parseInt(t));
            } else {
                cuerpo.put(clave, Double.parseDouble(t));
            }
        } catch (NumberFormatException e) {
            throw new JSONException("El valor «" + t + "» no es un número válido");
        }
    }

    /** Al corregir, un campo vacío se envía como null para que quede vacío en la donación. */
    private static void numeroEditable(JSONObject cuerpo, String clave, TextInputEditText campo, boolean entero) throws JSONException {
        if (texto(campo).isEmpty()) {
            cuerpo.put(clave, JSONObject.NULL);
        } else {
            numero(cuerpo, clave, campo, entero);
        }
    }

    private void guardarCorreccion(TipoDonacion tipo) {
        final String resultado = resultadoElegido();
        if ("DIFERIDO_TEMPORAL".equals(resultado) && diferidoHasta == null) {
            mostrarError(getString(R.string.donacion_falta_diferido_hasta));
            return;
        }
        if (resultado.startsWith("DIFERIDO") && texto(inputMotivo).isEmpty()) {
            mostrarError(getString(R.string.donacion_falta_motivo));
            return;
        }
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("version", donacionEditada.optInt("version", 1));
            cuerpo.put("tipo_donacion", tipo.getCodigo());
            cuerpo.put("fecha_hora", Fechas.aInstanteUtc(fecha, hora));
            cuerpo.put("se_completo", checkSeCompleto.isChecked());
            numeroEditable(cuerpo, "peso_dia_kg", inputPeso, false);
            numeroEditable(cuerpo, "temperatura_c", inputTemperatura, false);
            numeroEditable(cuerpo, "pa_antes_sistolica", inputSistolica, true);
            numeroEditable(cuerpo, "pa_antes_diastolica", inputDiastolica, true);
            numeroEditable(cuerpo, "frecuencia_cardiaca", inputPulso, true);
            numeroEditable(cuerpo, "recuento_plaquetas", inputPlaquetas, false);
            numeroEditable(cuerpo, "volumen_ml", inputVolumen, true);
            JSONObject hemograma = new JSONObject();
            numeroEditable(hemograma, "hemoglobina", inputHemoglobina, false);
            numeroEditable(hemograma, "hematocrito", inputHematocrito, false);
            cuerpo.put("hemograma", hemograma);
            cuerpo.put("observaciones", texto(inputObservaciones).isEmpty() ? JSONObject.NULL : texto(inputObservaciones));
            if (permisos.defineResultado()) {  // quien no puede definirlo no lo manda: la API conserva el actual
                cuerpo.put("resultado", resultado);
                cuerpo.put("motivo_diferimiento", resultado.startsWith("DIFERIDO") ? texto(inputMotivo) : JSONObject.NULL);
                cuerpo.put("diferido_hasta", "DIFERIDO_TEMPORAL".equals(resultado) ? diferidoHasta : JSONObject.NULL);
            }
        } catch (JSONException e) {
            mostrarError(e.getMessage());
            return;
        }
        botonGuardar.setEnabled(false);
        cargar(() -> api.objeto("POST", "/donaciones/" + donacionEditada.optLong("id") + "/corregir", cuerpo), r -> {
            aviso(getString(R.string.donacion_corregida));
            finish();
        }, () -> botonGuardar.setEnabled(true));
    }

    private void guardar() {
        Object elegido = spinnerTipo.getSelectedItem();
        if (donanteId == 0 || !(elegido instanceof TipoDonacion)) {
            return;
        }
        if (donacionEditada != null) {
            guardarCorreccion((TipoDonacion) elegido);
            return;
        }
        final String resultado = resultadoElegido();
        if ("DIFERIDO_TEMPORAL".equals(resultado) && diferidoHasta == null) {
            mostrarError(getString(R.string.donacion_falta_diferido_hasta));
            return;
        }
        if (resultado.startsWith("DIFERIDO") && texto(inputMotivo).isEmpty()) {
            mostrarError(getString(R.string.donacion_falta_motivo));
            return;
        }
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("donante_id", donanteId);
            if (turnoId != 0) {
                cuerpo.put("turno_id", turnoId);
            }
            cuerpo.put("tipo_donacion", ((TipoDonacion) elegido).getCodigo());
            cuerpo.put("fecha_hora", Fechas.aInstanteUtc(fecha, hora));
            cuerpo.put("resultado", resultado);
            cuerpo.put("se_completo", checkSeCompleto.isChecked());
            numero(cuerpo, "peso_dia_kg", inputPeso, false);
            numero(cuerpo, "temperatura_c", inputTemperatura, false);
            numero(cuerpo, "pa_antes_sistolica", inputSistolica, true);
            numero(cuerpo, "pa_antes_diastolica", inputDiastolica, true);
            numero(cuerpo, "frecuencia_cardiaca", inputPulso, true);
            numero(cuerpo, "recuento_plaquetas", inputPlaquetas, false);
            numero(cuerpo, "volumen_ml", inputVolumen, true);
            JSONObject hemograma = new JSONObject();
            numero(hemograma, "hemoglobina", inputHemoglobina, false);
            numero(hemograma, "hematocrito", inputHematocrito, false);
            if (hemograma.length() > 0) {
                cuerpo.put("hemograma", hemograma);
            }
            if (resultado.startsWith("DIFERIDO")) {
                cuerpo.put("motivo_diferimiento", texto(inputMotivo));
            }
            if ("DIFERIDO_TEMPORAL".equals(resultado)) {
                cuerpo.put("diferido_hasta", diferidoHasta);
            }
            String observaciones = texto(inputObservaciones);
            if (!observaciones.isEmpty()) {
                cuerpo.put("observaciones", observaciones);
            }
        } catch (JSONException e) {
            mostrarError(e.getMessage());
            return;
        }
        botonGuardar.setEnabled(false);
        cargar(() -> api.objeto("POST", "/donaciones/registrar", cuerpo), respuesta -> {
            JSONArray avisos = respuesta.optJSONArray("avisos");
            if (avisos == null || avisos.length() == 0) {
                aviso(getString(R.string.donacion_guardada));
                finish();
                return;
            }
            StringBuilder texto = new StringBuilder(getString(R.string.donacion_guardada_con_avisos));
            for (int i = 0; i < avisos.length(); i++) {
                texto.append("\n• ").append(avisos.optString(i));
            }
            new AlertDialog.Builder(this).setMessage(texto.toString())
                    .setPositiveButton(android.R.string.ok, (d, w) -> finish())
                    .setCancelable(false)
                    .show();
        }, () -> botonGuardar.setEnabled(true));
    }
}
