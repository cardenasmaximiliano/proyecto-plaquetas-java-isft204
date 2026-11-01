package com.hemogestion.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.hemogestion.app.R;
import com.hemogestion.app.api.HemoApi;
import com.hemogestion.app.util.Fechas;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Donantes para recontactar: la API evalúa a cada uno (elegibilidad a hoy, consentimiento y turno futuro).
 */
public class BusquedaAvanzadaActivity extends BaseActivity {

    private static final String[] GRUPOS = {"", "A", "B", "AB", "0"};
    private static final String[] FACTORES = {"", "+", "-"};

    private Spinner spinnerGrupo;
    private Spinner spinnerFactor;
    private TextView textoResumen;
    private LinearLayout listaCandidatos;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        if (!haySesion()) {
            return;
        }
        setContentView(R.layout.activity_busqueda);
        spinnerGrupo = findViewById(R.id.spinnerGrupo);
        spinnerFactor = findViewById(R.id.spinnerFactor);
        textoResumen = findViewById(R.id.textoResumen);
        listaCandidatos = findViewById(R.id.listaCandidatos);
        BarraInferior.configurar(this, BarraInferior.BUSQUEDA);

        spinnerGrupo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.grupo_filtro)));
        spinnerFactor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.factor_filtro)));
        findViewById(R.id.botonBuscar).setOnClickListener(v -> buscar());
        buscar();
    }

    private void buscar() {
        final String grupo = GRUPOS[Math.max(0, spinnerGrupo.getSelectedItemPosition())];
        final String factor = FACTORES[Math.max(0, spinnerFactor.getSelectedItemPosition())];
        textoResumen.setText(R.string.cargando);
        cargar(() -> {
            StringBuilder ruta = new StringBuilder("/recontacto?limite=200");
            if (!grupo.isEmpty()) {
                ruta.append("&grupo=").append(HemoApi.param(grupo));
            }
            if (!factor.isEmpty()) {
                ruta.append("&factor=").append(HemoApi.param(factor));
            }
            return api.objeto("GET", ruta.toString(), null);
        }, this::mostrar, () -> textoResumen.setText(""));
    }

    private void mostrar(JSONObject respuesta) {
        listaCandidatos.removeAllViews();
        JSONArray items = respuesta.optJSONArray("items");
        int listos = 0;
        for (int i = 0; items != null && i < items.length(); i++) {
            JSONObject d = items.optJSONObject(i);
            if (d == null) {
                continue;
            }
            listos += "LISTO".equals(d.optString("estado")) ? 1 : 0;
            listaCandidatos.addView(crearTarjeta(d));
        }
        textoResumen.setText(getString(R.string.busqueda_resumen, respuesta.optInt("evaluados"), listos));
    }

    private View crearTarjeta(JSONObject d) {
        final long id = d.optLong("donante_id");
        final String estado = d.optString("estado");
        View tarjeta = getLayoutInflater().inflate(R.layout.item_recontacto, listaCandidatos, false);
        TextView nombre = tarjeta.findViewById(R.id.textoNombre);
        nombre.setText(d.optString("apellido") + ", " + d.optString("nombre"));
        nombre.setOnClickListener(v -> {
            Intent intent = new Intent(this, DonanteDetalleActivity.class);
            intent.putExtra(DonanteDetalleActivity.EXTRA_ID, id);
            startActivity(intent);
        });
        String grupo = d.isNull("grupo") ? "" : d.optString("grupo") + (d.isNull("factor") ? "" : d.optString("factor"));
        TextView chipGrupo = tarjeta.findViewById(R.id.textoGrupo);
        chipGrupo.setText(grupo.isEmpty() ? "—" : grupo);
        String ultima = d.isNull("ultima_donacion") ? null : d.optString("ultima_donacion");
        ((TextView) tarjeta.findViewById(R.id.textoUltima)).setText(ultima == null ? getString(R.string.busqueda_ultima_sin_registro)
                : getString(R.string.busqueda_ultima, Fechas.visible(ultima)));

        TextView chip = tarjeta.findViewById(R.id.textoEstado);
        TextView motivo = tarjeta.findViewById(R.id.textoMotivo);
        int color;
        switch (estado) {
            case "LISTO":
                chip.setText(R.string.busqueda_listo);
                chip.setBackgroundResource(R.drawable.bg_chip_exito);
                color = R.color.hemo_exito;
                motivo.setText(R.string.busqueda_periodo_cumplido);
                break;
            case "RESTRICCION":
                chip.setText(R.string.busqueda_restriccion);
                chip.setBackgroundResource(R.drawable.bg_chip_error);
                color = R.color.hemo_error;
                motivo.setText(primerMotivo(d));
                break;
            case "EN_ESPERA":
                chip.setText(R.string.busqueda_en_espera);
                chip.setBackgroundResource(R.drawable.bg_chip_alerta);
                color = R.color.hemo_advertencia;
                motivo.setText(d.isNull("habilitado_desde") ? primerMotivo(d)
                        : getString(R.string.busqueda_habilitado_desde, Fechas.visible(d.optString("habilitado_desde"))));
                break;
            case "CON_TURNO":
                chip.setText(R.string.busqueda_con_turno);
                chip.setBackgroundResource(R.drawable.bg_chip_neutro);
                color = R.color.hemo_texto_secundario;
                motivo.setText(R.string.busqueda_ya_tiene_turno);
                break;
            default:
                chip.setText(R.string.busqueda_sin_historial);
                chip.setBackgroundResource(R.drawable.bg_chip_neutro);
                color = R.color.hemo_texto_secundario;
                motivo.setText(R.string.busqueda_sin_historial_detalle);
        }
        chip.setTextColor(ContextCompat.getColor(this, color));
        motivo.setTextColor(ContextCompat.getColor(this, color));
        return tarjeta;
    }

    private static String primerMotivo(JSONObject d) {
        JSONArray motivos = d.optJSONArray("motivos");
        return motivos != null && motivos.length() > 0 ? motivos.optString(0) : "";
    }
}
