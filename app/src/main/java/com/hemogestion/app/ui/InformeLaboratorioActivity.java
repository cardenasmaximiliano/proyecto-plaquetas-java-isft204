package com.hemogestion.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hemogestion.app.R;
import com.hemogestion.app.util.Texto;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Informe de laboratorio que trae el paciente. Opcional: al aceptar devuelve los valores al formulario del donante;
 * "Quitar informe" devuelve que no hay informe.
 */
public class InformeLaboratorioActivity extends AppCompatActivity {

    /** Entrada: el informe actual como JSON (o ausente si no hay). */
    public static final String EXTRA_INFORME = "informe";
    /** Salida: el informe como JSON, o ausente si se quitó. */
    public static final String EXTRA_RESULTADO = "informe_resultado";

    // Campos del informe con la clave que usa la API.
    private final Map<String, TextInputEditText> campos = new LinkedHashMap<>();
    private TextView textoError;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.activity_informe_laboratorio);

        campos.put("fecha_informe", findViewById(R.id.inputInformeFecha));
        campos.put("hemoglobina", findViewById(R.id.inputInformeHemoglobina));
        campos.put("hematocrito", findViewById(R.id.inputInformeHematocrito));
        campos.put("globulos_rojos", findViewById(R.id.inputInformeGlobulosRojos));
        campos.put("vcm", findViewById(R.id.inputInformeVcm));
        campos.put("hcm", findViewById(R.id.inputInformeHcm));
        campos.put("chcm", findViewById(R.id.inputInformeChcm));
        campos.put("globulos_blancos", findViewById(R.id.inputInformeGlobulosBlancos));
        campos.put("neutrofilos", findViewById(R.id.inputInformeNeutrofilos));
        campos.put("linfocitos", findViewById(R.id.inputInformeLinfocitos));
        campos.put("monocitos", findViewById(R.id.inputInformeMonocitos));
        campos.put("eosinofilos", findViewById(R.id.inputInformeEosinofilos));
        campos.put("basofilos", findViewById(R.id.inputInformeBasofilos));
        campos.put("plaquetas", findViewById(R.id.inputInformePlaquetas));
        textoError = findViewById(R.id.textoErrorInforme);

        cargar(intentoJson());

        findViewById(R.id.botonVolverInforme).setOnClickListener(v -> finish());
        findViewById(R.id.botonQuitarInforme).setOnClickListener(v -> devolver(null));
        findViewById(R.id.botonAceptarInforme).setOnClickListener(v -> aceptar());
    }

    private JSONObject intentoJson() {
        String texto = getIntent().getStringExtra(EXTRA_INFORME);
        if (texto == null) {
            return null;
        }
        try {
            return new JSONObject(texto);
        } catch (JSONException e) {
            return null;
        }
    }

    private void cargar(JSONObject informe) {
        for (Map.Entry<String, TextInputEditText> campo : campos.entrySet()) {
            Object valor = informe == null ? null : informe.opt(campo.getKey());
            campo.getValue().setText(valor == null || valor == JSONObject.NULL ? "" : valor.toString());
        }
    }

    private void aceptar() {
        if (!valido()) {
            textoError.setText(R.string.form_error_informe);
            textoError.setVisibility(View.VISIBLE);
            return;
        }
        JSONObject informe = new JSONObject();
        boolean hayDatos = false;
        try {
            for (Map.Entry<String, TextInputEditText> campo : campos.entrySet()) {
                String texto = textoDe(campo.getValue());
                if (texto.isEmpty()) {
                    continue;
                }
                hayDatos = true;
                informe.put(campo.getKey(), "fecha_informe".equals(campo.getKey()) ? texto : Double.parseDouble(texto));
            }
        } catch (JSONException e) {
            textoError.setText(R.string.form_error_informe);
            textoError.setVisibility(View.VISIBLE);
            return;
        }
        devolver(hayDatos ? informe : null);
    }

    /** La fecha como AAAA-MM-DD y los valores como números; los campos vacíos se ignoran. */
    private boolean valido() {
        for (Map.Entry<String, TextInputEditText> campo : campos.entrySet()) {
            String texto = textoDe(campo.getValue());
            if (texto.isEmpty()) {
                continue;
            }
            if ("fecha_informe".equals(campo.getKey())) {
                if (!Texto.esFecha(texto)) {
                    return false;
                }
            } else {
                try {
                    Double.parseDouble(texto);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return true;
    }

    private void devolver(JSONObject informe) {
        Intent resultado = new Intent();
        if (informe != null) {
            resultado.putExtra(EXTRA_RESULTADO, informe.toString());
        }
        setResult(RESULT_OK, resultado);
        finish();
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim().replace(',', '.');
    }
}
