package com.hemogestion.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
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

import org.json.JSONArray;
import org.json.JSONObject;

public class DonantesActivity extends AppCompatActivity {

    private static final int TAMANO_PAGINA = 50;

    private HemoApi api;
    private TextInputEditText inputBuscar;
    private TextView textoCantidad;
    private LinearLayout listaDonantes;
    private MaterialButton botonCargarMas;

    private String consulta = "";
    private int proximoOffset = 0;
    private int generacion = 0;   // cambia en cada búsqueda nueva: descarta respuestas viejas
    private boolean cargando = false;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        api = new HemoApi(this);
        if (!api.sesion().activa()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_donantes);
        Bordes.aplicar(this);
        inputBuscar = findViewById(R.id.inputBuscar);
        textoCantidad = findViewById(R.id.textoCantidad);
        listaDonantes = findViewById(R.id.listaDonantes);
        botonCargarMas = findViewById(R.id.botonCargarMas);

        inputBuscar.setOnEditorActionListener((vista, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_SEARCH || esEnter(evento)) {
                buscar(true);
                return true;
            }
            return false;
        });
        botonCargarMas.setOnClickListener(v -> buscar(false));
        findViewById(R.id.botonNuevo).setOnClickListener(v -> startActivity(new Intent(this, DonanteFormActivity.class)));
        findViewById(R.id.botonVolver).setOnClickListener(v -> finish());
        BarraInferior.configurar(this, BarraInferior.INICIO);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (api.sesion().activa()) {
            buscar(true);  // refresca al volver de la ficha o del formulario
        }
    }

    private void buscar(boolean nueva) {
        if (cargando) {
            return;
        }
        if (nueva) {
            consulta = textoDe(inputBuscar);
            proximoOffset = 0;
            generacion++;
            listaDonantes.removeAllViews();
        }
        final int generacionActual = generacion;
        final int offset = proximoOffset;
        final String texto = consulta;
        cargando = true;
        botonCargarMas.setEnabled(false);
        Hilos.enFondo(() -> {
            try {
                JSONObject respuesta = api.buscarDonantes(texto, offset, TAMANO_PAGINA);
                runOnUiThread(() -> mostrarPagina(generacionActual, respuesta));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    cargando = false;
                    botonCargarMas.setEnabled(true);
                    if (!Errores.volverALoginSiVencida(this, e)) {
                        Toast.makeText(this, Errores.mensaje(this, e), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void mostrarPagina(int generacionRespuesta, JSONObject respuesta) {
        cargando = false;
        if (generacionRespuesta != generacion) {
            return;  // llegó tarde: ya se hizo otra búsqueda
        }
        try {
            int total = respuesta.getInt("total");
            JSONArray items = respuesta.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                listaDonantes.addView(crearTarjeta(Donante.desdeJson(items.getJSONObject(i))));
            }
            proximoOffset += items.length();
            textoCantidad.setText(total == 0 ? getString(R.string.donantes_vacio) : getString(R.string.donantes_cantidad, total));
            botonCargarMas.setVisibility(proximoOffset < total ? View.VISIBLE : View.GONE);
            botonCargarMas.setEnabled(true);
        } catch (Exception e) {
            Toast.makeText(this, Errores.mensaje(this, e), Toast.LENGTH_LONG).show();
        }
    }

    private View crearTarjeta(Donante donante) {
        View tarjeta = getLayoutInflater().inflate(R.layout.item_donante, listaDonantes, false);
        ((TextView) tarjeta.findViewById(R.id.textoNombre)).setText(donante.nombreCompleto());
        ((TextView) tarjeta.findViewById(R.id.textoDetalle))
                .setText(getString(R.string.donante_dni, donante.texto("numero_documento")) + "    " + donante.grupoFactor());
        tarjeta.setOnClickListener(v -> {
            Intent intent = new Intent(this, DonanteDetalleActivity.class);
            intent.putExtra(DonanteDetalleActivity.EXTRA_ID, donante.getId());
            startActivity(intent);
        });
        return tarjeta;
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }

    /** Enter de un teclado físico (el del emulador, por ejemplo) también busca. */
    private static boolean esEnter(KeyEvent evento) {
        return evento != null && evento.getKeyCode() == KeyEvent.KEYCODE_ENTER && evento.getAction() == KeyEvent.ACTION_DOWN;
    }
}
