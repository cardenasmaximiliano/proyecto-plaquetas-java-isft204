package com.hemogestion.app.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.hemogestion.app.R;
import com.hemogestion.app.api.HemoApi;
import com.hemogestion.app.modelo.Donante;
import com.hemogestion.app.modelo.TipoDonacion;
import com.hemogestion.app.util.Fechas;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Agenda un turno: se elige el donante, el tipo de donación, el día y uno de los horarios con cupo.
 * Las reglas (donante habilitado, un solo turno activo, cupos) las aplica la API y sus mensajes se muestran tal cual.
 */
public class AgendarActivity extends BaseActivity {

    public static final String EXTRA_DONANTE_ID = "donante_id";
    public static final String EXTRA_DONANTE_NOMBRE = "donante_nombre";
    public static final String EXTRA_FECHA = "fecha";
    /** Si viene, la pantalla reprograma ese turno (mismo donante y tipo) en lugar de agendar uno nuevo. */
    public static final String EXTRA_TURNO_ID = "turno_id";
    public static final String EXTRA_TIPO = "tipo";

    private TextView textoDonante;
    private TextInputEditText inputBuscar;
    private LinearLayout resultados;
    private Spinner spinnerTipo;
    private MaterialButton botonFecha;
    private TextView textoSinHorarios;
    private ChipGroup chipsHorarios;
    private TextInputEditText inputObservaciones;
    private MaterialButton botonConfirmar;

    private long donanteId;
    private long turnoId;
    private String tipoPedido;
    private String fecha;
    private String horaElegida;
    private List<TipoDonacion> tipos;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        if (!haySesion()) {
            return;
        }
        setContentView(R.layout.activity_agendar);
        textoDonante = findViewById(R.id.textoDonante);
        inputBuscar = findViewById(R.id.inputBuscar);
        resultados = findViewById(R.id.resultados);
        spinnerTipo = findViewById(R.id.spinnerTipo);
        botonFecha = findViewById(R.id.botonFecha);
        textoSinHorarios = findViewById(R.id.textoSinHorarios);
        chipsHorarios = findViewById(R.id.chipsHorarios);
        inputObservaciones = findViewById(R.id.inputObservaciones);
        botonConfirmar = findViewById(R.id.botonConfirmarAgenda);

        String hoy = Fechas.hoy();
        String pedida = getIntent().getStringExtra(EXTRA_FECHA);
        fecha = pedida != null && pedida.compareTo(hoy) >= 0 ? pedida : hoy;
        mostrarFecha();

        donanteId = getIntent().getLongExtra(EXTRA_DONANTE_ID, 0);
        if (donanteId != 0) {
            elegirDonante(donanteId, getIntent().getStringExtra(EXTRA_DONANTE_NOMBRE));
            findViewById(R.id.capaBuscar).setVisibility(View.GONE);  // viene de la ficha: ya se sabe quién es
        }

        turnoId = getIntent().getLongExtra(EXTRA_TURNO_ID, 0);
        tipoPedido = getIntent().getStringExtra(EXTRA_TIPO);
        if (turnoId != 0) {
            prepararReprogramacion();
        }

        findViewById(R.id.botonVolver).setOnClickListener(v -> finish());
        botonFecha.setOnClickListener(v -> elegirFecha());
        botonConfirmar.setOnClickListener(v -> agendar());
        inputBuscar.setOnEditorActionListener((vista, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_SEARCH || esEnter(evento)) {
                buscarDonantes();
                return true;
            }
            return false;
        });
        spinnerTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> padre, View vista, int posicion, long id) {
                consultarHorarios();
            }

            @Override
            public void onNothingSelected(AdapterView<?> padre) {
            }
        });
        cargar(() -> api.tiposDonacion(), lista -> {
            tipos = lista;
            ArrayAdapter<TipoDonacion> adaptador = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, lista);
            spinnerTipo.setAdapter(adaptador);
            for (int i = 0; tipoPedido != null && i < lista.size(); i++) {
                if (lista.get(i).getCodigo().equals(tipoPedido)) {
                    spinnerTipo.setSelection(i);
                }
            }
        });
        actualizarBoton();
    }

    @Override
    protected void mostrarError(String mensaje) {
        // Los motivos por los que un donante no puede donar son varias líneas: mejor en un diálogo que en un aviso.
        new AlertDialog.Builder(this).setMessage(mensaje).setPositiveButton(android.R.string.ok, null).show();
    }

    /** Al reprogramar se conserva el donante y el tipo: solo se elige el nuevo día y horario. */
    private void prepararReprogramacion() {
        ((TextView) findViewById(R.id.textoTitulo)).setText(R.string.agendar_reprogramar_titulo);
        findViewById(R.id.capaBuscar).setVisibility(View.GONE);
        findViewById(R.id.resultados).setVisibility(View.GONE);
        findViewById(R.id.capaObservaciones).setVisibility(View.GONE);
        spinnerTipo.setEnabled(false);
        botonConfirmar.setText(R.string.agendar_reprogramar_confirmar);
    }

    private void mostrarFecha() {
        botonFecha.setText(getString(R.string.agendar_fecha, Fechas.visible(fecha)));
    }

    private void elegirFecha() {
        DatePickerDialog dialogo = new DatePickerDialog(this, (vista, anio, mes, dia) -> {
            fecha = Fechas.armar(anio, mes + 1, dia);
            mostrarFecha();
            consultarHorarios();
        }, Fechas.anio(fecha), Fechas.mes(fecha) - 1, Fechas.diaDelMes(fecha));
        dialogo.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialogo.show();
    }

    private void buscarDonantes() {
        final String consulta = texto(inputBuscar);
        cargar(() -> api.buscarDonantes(consulta, 0, 20), respuesta -> {
            resultados.removeAllViews();
            try {
                JSONArray items = respuesta.getJSONArray("items");
                if (items.length() == 0) {
                    aviso(getString(R.string.donantes_vacio));
                }
                for (int i = 0; i < items.length(); i++) {
                    Donante donante = Donante.desdeJson(items.getJSONObject(i));
                    View fila = getLayoutInflater().inflate(R.layout.item_donante, resultados, false);
                    ((TextView) fila.findViewById(R.id.textoNombre)).setText(donante.nombreCompleto());
                    ((TextView) fila.findViewById(R.id.textoDetalle))
                            .setText(getString(R.string.donante_dni, donante.texto("numero_documento")) + "    " + donante.grupoFactor());
                    fila.setOnClickListener(v -> {
                        elegirDonante(donante.getId(), donante.nombreCompleto());
                        resultados.removeAllViews();
                    });
                    resultados.addView(fila);
                }
            } catch (Exception e) {
                mostrarError(e.getMessage());
            }
        });
    }

    private void elegirDonante(long id, String nombre) {
        donanteId = id;
        textoDonante.setText(nombre == null ? getString(R.string.agendar_ningun_donante) : nombre);
        actualizarBoton();
    }

    private TipoDonacion tipoElegido() {
        Object elegido = spinnerTipo.getSelectedItem();
        return elegido instanceof TipoDonacion ? (TipoDonacion) elegido : null;
    }

    private void consultarHorarios() {
        TipoDonacion tipo = tipoElegido();
        horaElegida = null;
        chipsHorarios.removeAllViews();
        textoSinHorarios.setVisibility(View.GONE);
        actualizarBoton();
        if (tipo == null) {
            return;
        }
        final String dia = fecha;
        cargar(() -> api.lista("/agenda/disponibilidad?fecha=" + HemoApi.param(dia) + "&tipo=" + HemoApi.param(tipo.getCodigo())), franjas -> {
            if (!dia.equals(fecha) || tipo != tipoElegido()) {
                return;  // llegó tarde: ya se cambió el día o el tipo
            }
            mostrarHorarios(franjas);
        });
    }

    private void mostrarHorarios(JSONArray franjas) {
        chipsHorarios.removeAllViews();
        textoSinHorarios.setVisibility(franjas.length() == 0 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < franjas.length(); i++) {
            JSONObject f = franjas.optJSONObject(i);
            if (f == null) {
                continue;
            }
            final String hora = f.optString("inicio");
            final int libres = f.optInt("libres");
            Chip chip = new Chip(this);
            chip.setText(libres > 0 ? getString(R.string.agendar_franja_libres, hora, libres) : getString(R.string.agendar_franja_completa, hora));
            chip.setCheckable(true);
            chip.setEnabled(libres > 0);
            chip.setOnClickListener(v -> {
                horaElegida = hora;
                actualizarBoton();
            });
            chipsHorarios.addView(chip);
        }
    }

    private void actualizarBoton() {
        botonConfirmar.setEnabled(donanteId != 0 && horaElegida != null && tipoElegido() != null);
    }

    private void agendar() {
        TipoDonacion tipo = tipoElegido();
        if (donanteId == 0 || horaElegida == null || tipo == null) {
            return;
        }
        botonConfirmar.setEnabled(false);
        final boolean reprogramar = turnoId != 0;
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("fecha", fecha);
            cuerpo.put("hora", horaElegida);
            if (!reprogramar) {
                cuerpo.put("donante_id", donanteId);
                cuerpo.put("tipo", tipo.getCodigo());
                cuerpo.put("origen", "PRESENCIAL");
                String observaciones = texto(inputObservaciones);
                if (!observaciones.isEmpty()) {
                    cuerpo.put("observaciones", observaciones);
                }
            }
        } catch (Exception e) {
            mostrarError(e.getMessage());
            return;
        }
        final String ruta = reprogramar ? "/turnos/" + turnoId + "/reprogramar" : "/agenda/turnos";
        cargar(() -> api.objeto("POST", ruta, cuerpo), turno -> {
            aviso(getString(reprogramar ? R.string.agendar_reprogramado : R.string.agendar_listo));
            finish();
        }, () -> {
            actualizarBoton();
            consultarHorarios();  // los cupos pudieron cambiar
        });
    }

    /** Enter de un teclado físico (el del emulador, por ejemplo) también busca. */
    private static boolean esEnter(KeyEvent evento) {
        return evento != null && evento.getKeyCode() == KeyEvent.KEYCODE_ENTER && evento.getAction() == KeyEvent.ACTION_DOWN;
    }
}
