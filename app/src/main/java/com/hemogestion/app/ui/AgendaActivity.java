package com.hemogestion.app.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.textfield.TextInputEditText;
import com.hemogestion.app.R;
import com.hemogestion.app.api.HemoApi;
import com.hemogestion.app.util.Fechas;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Turnos de la semana: se elige el día, se ve quién viene y se confirma, cancela o se registra la donación. */
public class AgendaActivity extends BaseActivity {

    private LinearLayout tiraDias;
    private LinearLayout listaTurnos;
    private TextView textoSemana;
    private TextView textoFecha;
    private TextView textoCantidad;

    /** Cuántos días muestra la tira de la agenda; las flechas la corren de a ese mismo paso. */
    private static final int DIAS_VISIBLES = 10;

    private String lunes;
    private String diaElegido;
    private final Map<Long, String> nombres = new ConcurrentHashMap<>();

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        if (!haySesion()) {
            return;
        }
        setContentView(R.layout.activity_agenda);
        tiraDias = findViewById(R.id.tiraDias);
        listaTurnos = findViewById(R.id.listaTurnos);
        textoSemana = findViewById(R.id.textoSemana);
        textoFecha = findViewById(R.id.textoFecha);
        textoCantidad = findViewById(R.id.textoCantidad);
        BarraInferior.configurar(this, BarraInferior.AGENDA);

        diaElegido = Fechas.hoy();
        lunes = Fechas.lunesDe(diaElegido);
        findViewById(R.id.botonSemanaAnterior).setOnClickListener(v -> cambiarSemana(-DIAS_VISIBLES));
        findViewById(R.id.botonSemanaSiguiente).setOnClickListener(v -> cambiarSemana(DIAS_VISIBLES));
        findViewById(R.id.botonAgendar).setOnClickListener(v -> {
            Intent intent = new Intent(this, AgendarActivity.class);
            intent.putExtra(AgendarActivity.EXTRA_FECHA, diaElegido);
            startActivity(intent);
        });
        View botonWhatsApp = findViewById(R.id.botonWhatsAppPendientes);
        botonWhatsApp.setVisibility(permisos.atiende() ? View.VISIBLE : View.GONE);
        botonWhatsApp.setOnClickListener(v -> confirmarRecordatoriosWhatsApp());
        dibujarTira();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (haySesion()) {
            cargarTurnos();
        }
    }

    private void cambiarSemana(int dias) {
        lunes = Fechas.sumarDias(lunes, dias);
        diaElegido = lunes;
        dibujarTira();
        cargarTurnos();
    }

    private void dibujarTira() {
        textoSemana.setText(getString(R.string.agenda_semana, Fechas.visible(lunes), Fechas.visible(Fechas.sumarDias(lunes, DIAS_VISIBLES - 1))));
        tiraDias.removeAllViews();
        for (int i = 0; i < DIAS_VISIBLES; i++) {
            final String dia = Fechas.sumarDias(lunes, i);
            TextView vista = (TextView) getLayoutInflater().inflate(R.layout.item_dia, tiraDias, false);
            vista.setText(Fechas.etiquetaDia(dia));
            vista.setSelected(dia.equals(diaElegido));
            vista.setOnClickListener(v -> {
                diaElegido = dia;
                dibujarTira();
                cargarTurnos();
            });
            tiraDias.addView(vista);
        }
    }

    private void cargarTurnos() {
        final String dia = diaElegido;
        textoFecha.setText(getString(R.string.agenda_turnos_del, Fechas.visible(dia)));
        cargar(() -> {
            String ruta = "/turnos?desde=" + HemoApi.param(Fechas.inicioDelDiaUtc(dia))
                    + "&hasta=" + HemoApi.param(Fechas.inicioDelDiaUtc(Fechas.sumarDias(dia, 1)));
            JSONArray turnos = api.lista(ruta);
            api.tiposDonacion();
            for (int i = 0; i < turnos.length(); i++) {
                long donanteId = turnos.getJSONObject(i).getLong("donante_id");
                if (!nombres.containsKey(donanteId)) {
                    nombres.put(donanteId, api.obtenerDonante(donanteId).nombreCompleto());
                }
            }
            return turnos;
        }, turnos -> {
            if (dia.equals(diaElegido)) {
                mostrar(turnos);
            }
        });
    }

    private void mostrar(JSONArray turnos) {
        listaTurnos.removeAllViews();
        textoCantidad.setText(turnos.length() == 0 ? getString(R.string.agenda_sin_turnos)
                : getString(R.string.agenda_cantidad, turnos.length(), Fechas.visible(diaElegido)));
        for (int i = 0; i < turnos.length(); i++) {
            JSONObject turno = turnos.optJSONObject(i);
            if (turno != null) {
                listaTurnos.addView(crearTarjeta(turno));
            }
        }
    }

    private View crearTarjeta(JSONObject turno) {
        View tarjeta = getLayoutInflater().inflate(R.layout.item_turno, listaTurnos, false);
        String estado = turno.optString("estado");
        ((TextView) tarjeta.findViewById(R.id.textoHora)).setText(Fechas.hora(turno.optString("fecha_hora")));
        String nombre = nombres.get(turno.optLong("donante_id"));
        ((TextView) tarjeta.findViewById(R.id.textoDonante)).setText(nombre == null ? "—" : nombre);
        ((TextView) tarjeta.findViewById(R.id.textoTipo)).setText(api.nombreDelTipo(turno.optString("tipo_donacion")));
        TextView chip = tarjeta.findViewById(R.id.textoEstado);
        chip.setText(nombreDelEstado(estado));
        chip.setBackgroundResource(fondoDelEstado(estado));
        chip.setTextColor(androidx.core.content.ContextCompat.getColor(this, colorDelEstado(estado)));
        tarjeta.setOnClickListener(v -> mostrarAcciones(turno, nombre == null ? "" : nombre));
        return tarjeta;
    }

    static String nombreDelEstado(String estado) {
        switch (estado) {
            case "PENDIENTE":
                return "Pendiente";
            case "CONFIRMADO":
                return "Confirmado";
            case "CANCELADO":
                return "Cancelado";
            case "ASISTIO":
                return "Asistió";
            case "NO_ASISTIO":
                return "No asistió";
            default:
                return estado;
        }
    }

    private static int fondoDelEstado(String estado) {
        switch (estado) {
            case "CONFIRMADO":
            case "ASISTIO":
                return R.drawable.bg_chip_exito;
            case "CANCELADO":
                return R.drawable.bg_chip_error;
            case "NO_ASISTIO":
                return R.drawable.bg_chip_alerta;
            default:
                return R.drawable.bg_chip_neutro;
        }
    }

    private static int colorDelEstado(String estado) {
        switch (estado) {
            case "CONFIRMADO":
            case "ASISTIO":
                return R.color.hemo_exito;
            case "CANCELADO":
                return R.color.hemo_error;
            case "NO_ASISTIO":
                return R.color.hemo_advertencia;
            default:
                return R.color.hemo_texto_secundario;
        }
    }

    // ---- Acciones sobre un turno -----------------------------------------------------------------------------

    /** Click-to-chat: la API arma el mensaje del turno y acá se abre WhatsApp; la persona solo toca "Enviar". */
    private void abrirWhatsApp(long turnoId) {
        cargar(() -> api.objeto("GET", "/turnos/" + turnoId + "/whatsapp-enlace", null), r -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(r.optString("url"))));
            } catch (ActivityNotFoundException e) {
                mostrarError(getString(R.string.whatsapp_no_instalado));
            }
        });
    }

    /** Envío directo: el servidor manda el recordatorio del turno por el número vinculado (OpenWA). */
    private void enviarWhatsApp(long turnoId) {
        cargar(() -> api.objeto("POST", "/turnos/" + turnoId + "/whatsapp-enviar", new JSONObject()), r -> {
            String nombre = r.optString("nombre");
            String texto = "ENVIADO".equals(r.optString("estado"))
                    ? getString(R.string.whatsapp_enviado, nombre, r.optString("destino"))
                    : getString(R.string.whatsapp_no_enviado, nombre, r.optString("error"));
            new AlertDialog.Builder(this).setMessage(texto).setPositiveButton(android.R.string.ok, null).show();
        });
    }

    /** Envío masivo a demanda: arma los recordatorios que corresponden y el servidor los envía en segundo plano. */
    private void confirmarRecordatoriosWhatsApp() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.whatsapp_confirmar_masivo)
                .setPositiveButton(R.string.whatsapp_enviar, (dialogo, w) ->
                        cargar(() -> api.objeto("POST", "/whatsapp/enviar-pendientes", new JSONObject()), r -> {
                            String texto = r.optInt("pendientes") == 0
                                    ? getString(R.string.whatsapp_sin_pendientes)
                                    : getString(R.string.whatsapp_resumen, r.optInt("pendientes"), r.optInt("programados"));
                            if (r.optInt("omitidos") > 0) {
                                texto += "\n" + getString(R.string.whatsapp_resumen_omitidos, r.optInt("omitidos"));
                            }
                            new AlertDialog.Builder(this).setMessage(texto).setPositiveButton(android.R.string.ok, null).show();
                        }))
                .setNegativeButton(R.string.volver, null)
                .show();
    }

    private void mostrarAcciones(JSONObject turno, String nombre) {
        final long id = turno.optLong("id");
        final long donanteId = turno.optLong("donante_id");
        final String estado = turno.optString("estado");
        final boolean activo = "PENDIENTE".equals(estado) || "CONFIRMADO".equals(estado);

        List<String> textos = new ArrayList<>();
        List<Runnable> acciones = new ArrayList<>();
        if ("PENDIENTE".equals(estado)) {
            textos.add(getString(R.string.accion_confirmar));
            acciones.add(() -> cambiarEstado(id, "confirmar", null, null));
        }
        if (activo || "ASISTIO".equals(estado)) {
            textos.add(getString(R.string.accion_registrar_donacion));
            acciones.add(() -> {
                Intent intent = new Intent(this, DonacionFormActivity.class);
                intent.putExtra(DonacionFormActivity.EXTRA_DONANTE_ID, donanteId);
                intent.putExtra(DonacionFormActivity.EXTRA_DONANTE_NOMBRE, nombre);
                intent.putExtra(DonacionFormActivity.EXTRA_TURNO_ID, id);
                intent.putExtra(DonacionFormActivity.EXTRA_TIPO, turno.optString("tipo_donacion"));
                startActivity(intent);
            });
        }
        if (activo) {
            if (permisos.atiende()) {
                textos.add(getString(R.string.accion_whatsapp_enviar));
                acciones.add(() -> enviarWhatsApp(id));
                textos.add(getString(R.string.accion_whatsapp));
                acciones.add(() -> abrirWhatsApp(id));
            }
            textos.add(getString(R.string.accion_no_asistio));
            acciones.add(() -> cambiarEstado(id, "no_asistio", null, null));
            textos.add(getString(R.string.accion_reprogramar));
            acciones.add(() -> {
                Intent intent = new Intent(this, AgendarActivity.class);
                intent.putExtra(AgendarActivity.EXTRA_TURNO_ID, id);
                intent.putExtra(AgendarActivity.EXTRA_DONANTE_ID, donanteId);
                intent.putExtra(AgendarActivity.EXTRA_DONANTE_NOMBRE, nombre);
                intent.putExtra(AgendarActivity.EXTRA_TIPO, turno.optString("tipo_donacion"));
                intent.putExtra(AgendarActivity.EXTRA_FECHA, diaElegido);
                startActivity(intent);
            });
            textos.add(getString(R.string.accion_cancelar));
            acciones.add(() -> pedirMotivoDeCancelacion(id, nombre, turno.optString("fecha_hora")));
        }
        textos.add(getString(R.string.accion_ver_ficha));
        acciones.add(() -> {
            Intent intent = new Intent(this, DonanteDetalleActivity.class);
            intent.putExtra(DonanteDetalleActivity.EXTRA_ID, donanteId);
            startActivity(intent);
        });

        new AlertDialog.Builder(this)
                .setTitle(nombre + " · " + Fechas.hora(turno.optString("fecha_hora")))
                .setItems(textos.toArray(new String[0]), (dialogo, posicion) -> acciones.get(posicion).run())
                .show();
    }

    private void cambiarEstado(long turnoId, String accion, String motivoTipo, String motivo) {
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("accion", accion);
            if (motivoTipo != null) {
                cuerpo.put("motivo_tipo", motivoTipo);
                cuerpo.put("motivo", motivo);
            }
        } catch (JSONException e) {
            mostrarError(e.getMessage());
            return;
        }
        cargar(() -> api.objeto("POST", "/turnos/" + turnoId + "/accion", cuerpo), r -> {
            aviso(getString(R.string.agenda_turno_actualizado, nombreDelEstado(r.optString("estado"))));
            cargarTurnos();
        });
    }

    private void pedirMotivoDeCancelacion(long turnoId, String nombre, String fechaHora) {
        View contenido = getLayoutInflater().inflate(R.layout.dialog_cancelar_turno, null);
        ((TextView) contenido.findViewById(R.id.textoTurno)).setText(getString(R.string.cancelar_turno_de, nombre, Fechas.fechaHora(fechaHora)));
        final TextInputEditText detalle = contenido.findViewById(R.id.inputDetalle);
        final RadioButton otro = contenido.findViewById(R.id.motivoOtro);
        final RadioButton cancelo = contenido.findViewById(R.id.motivoCancelo);
        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(R.string.accion_cancelar)
                .setView(contenido)
                .setPositiveButton(R.string.cancelar_confirmar, null)
                .setNegativeButton(R.string.volver, null)
                .create();
        dialogo.show();
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String texto = texto(detalle);
            if (otro.isChecked() && texto.isEmpty()) {
                detalle.setError(getString(R.string.cancelar_detalle_obligatorio));
                return;
            }
            String tipo = otro.isChecked() ? "OTRO" : cancelo.isChecked() ? "CANCELO_DONANTE" : "NO_ASISTIO_LLAMADO";
            dialogo.dismiss();
            cambiarEstado(turnoId, "cancelar", tipo, texto);
        });
    }
}
