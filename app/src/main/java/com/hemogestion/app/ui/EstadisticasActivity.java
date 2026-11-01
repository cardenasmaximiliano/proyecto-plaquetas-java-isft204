package com.hemogestion.app.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.hemogestion.app.R;
import com.hemogestion.app.api.HemoApi;
import com.hemogestion.app.util.Fechas;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/** Resumen del centro en un período: donaciones, donantes, turnos, tendencia y procedimientos (director y superadmin). */
public class EstadisticasActivity extends BaseActivity {

    private MaterialButton botonDesde;
    private MaterialButton botonHasta;
    private Spinner spinnerAgrupacion;
    private TextView textoPeriodo;
    private LinearLayout listaResumen;
    private LinearLayout listaTendencia;
    private LinearLayout listaProcedimientos;

    private String desde;
    private String hasta;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        if (!haySesion()) {
            return;
        }
        setContentView(R.layout.activity_estadisticas);
        BarraInferior.configurar(this, BarraInferior.ESTADISTICAS);
        if (!permisos.veInformes()) {
            findViewById(R.id.bloqueContenido).setVisibility(View.GONE);
            findViewById(R.id.textoSinAcceso).setVisibility(View.VISIBLE);
            return;
        }
        botonDesde = findViewById(R.id.botonDesde);
        botonHasta = findViewById(R.id.botonHasta);
        spinnerAgrupacion = findViewById(R.id.spinnerAgrupacion);
        textoPeriodo = findViewById(R.id.textoPeriodo);
        listaResumen = findViewById(R.id.listaResumen);
        listaTendencia = findViewById(R.id.listaTendencia);
        listaProcedimientos = findViewById(R.id.listaProcedimientos);

        // Por defecto: desde el primer día del mes de hace 11 meses hasta hoy (un año de datos).
        hasta = Fechas.hoy();
        int mes = Fechas.mes(hasta) - 11;
        int anio = Fechas.anio(hasta);
        if (mes < 1) {
            mes += 12;
            anio -= 1;
        }
        desde = Fechas.armar(anio, mes, 1);
        mostrarFechas();

        spinnerAgrupacion.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.agrupacion_nombres)));
        spinnerAgrupacion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> padre, View vista, int posicion, long id) {
                consultar();
            }

            @Override
            public void onNothingSelected(AdapterView<?> padre) {
            }
        });
        botonDesde.setOnClickListener(v -> elegirFecha(true));
        botonHasta.setOnClickListener(v -> elegirFecha(false));
    }

    private void mostrarFechas() {
        botonDesde.setText(getString(R.string.estadisticas_desde, Fechas.visible(desde)));
        botonHasta.setText(getString(R.string.estadisticas_hasta, Fechas.visible(hasta)));
    }

    private void elegirFecha(boolean esDesde) {
        String base = esDesde ? desde : hasta;
        new DatePickerDialog(this, (vista, anio, mes, dia) -> {
            String elegida = Fechas.armar(anio, mes + 1, dia);
            if (esDesde) {
                desde = elegida;
            } else {
                hasta = elegida;
            }
            mostrarFechas();
            consultar();
        }, Fechas.anio(base), Fechas.mes(base) - 1, Fechas.diaDelMes(base)).show();
    }

    private void consultar() {
        final String agrupacion = spinnerAgrupacion.getSelectedItemPosition() == 1 ? "semana" : "mes";
        final String d = desde;
        final String h = hasta;
        cargar(() -> {
            api.tiposDonacion();
            return api.objeto("GET", "/informes/resumen?desde=" + HemoApi.param(d) + "&hasta=" + HemoApi.param(h)
                    + "&agrupacion=" + agrupacion, null);
        }, this::mostrar);
    }

    private void mostrar(JSONObject r) {
        textoPeriodo.setText(getString(R.string.estadisticas_periodo, Fechas.visible(r.optString("desde")), Fechas.visible(r.optString("hasta"))));

        listaResumen.removeAllViews();
        fila(listaResumen, getString(R.string.estadisticas_donaciones), String.valueOf(r.optInt("donaciones")));
        fila(listaResumen, getString(R.string.estadisticas_donantes_unicos), String.valueOf(r.optInt("donantes_unicos")));
        fila(listaResumen, getString(R.string.estadisticas_donantes_sexo), r.optInt("donantes_mujeres") + " / " + r.optInt("donantes_hombres"));
        fila(listaResumen, getString(R.string.estadisticas_nuevos), String.valueOf(r.optInt("donantes_nuevos")));
        fila(listaResumen, getString(R.string.estadisticas_asistidos), String.valueOf(r.optInt("turnos_asistieron")));
        fila(listaResumen, getString(R.string.estadisticas_no_asistidos), String.valueOf(r.optInt("turnos_no_asistieron")));
        fila(listaResumen, getString(R.string.estadisticas_tasa), String.format(new Locale("es", "AR"), "%.1f%%", r.optDouble("tasa_inasistencia")));

        listaTendencia.removeAllViews();
        JSONArray tendencia = r.optJSONArray("tendencia");
        for (int i = 0; tendencia != null && i < tendencia.length(); i++) {
            JSONObject t = tendencia.optJSONObject(i);
            if (t != null && t.optInt("donaciones") > 0) {
                fila(listaTendencia, t.optString("etiqueta"), String.valueOf(t.optInt("donaciones")));
            }
        }
        if (listaTendencia.getChildCount() == 0) {
            listaTendencia.addView(mensaje(getString(R.string.estadisticas_sin_donaciones)));
        }

        listaProcedimientos.removeAllViews();
        JSONArray procedimientos = r.optJSONArray("por_procedimiento");
        for (int i = 0; procedimientos != null && i < procedimientos.length(); i++) {
            JSONObject p = procedimientos.optJSONObject(i);
            if (p != null) {
                fila(listaProcedimientos, p.optString("nombre"), String.valueOf(p.optInt("cantidad")));
            }
        }
        if (listaProcedimientos.getChildCount() == 0) {
            listaProcedimientos.addView(mensaje(getString(R.string.estadisticas_sin_donaciones)));
        }
    }

    private void fila(LinearLayout contenedor, String etiqueta, String valor) {
        View fila = getLayoutInflater().inflate(R.layout.item_estadistica, contenedor, false);
        ((TextView) fila.findViewById(R.id.textoEtiqueta)).setText(etiqueta);
        ((TextView) fila.findViewById(R.id.textoValor)).setText(valor);
        contenedor.addView(fila);
    }

    private TextView mensaje(String texto) {
        TextView vista = new TextView(this);
        vista.setText(texto);
        return vista;
    }
}
