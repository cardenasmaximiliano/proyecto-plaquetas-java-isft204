package com.hemogestion.app.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hemogestion.app.R;
import com.hemogestion.app.util.Fechas;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo para elegir una restricción: las tres fijas (tatuaje, cirugía reciente, bajo peso) y las del catálogo
 * del centro. Lo usan la ficha del donante y el alta/edición, para que las dos pantallas funcionen igual.
 */
final class DialogoRestriccion {

    private DialogoRestriccion() {
    }

    /** Lo que se hace con la restricción elegida (ya armada como la espera la API). */
    interface AlElegir {
        void con(JSONObject restriccion);
    }

    /** Una opción del desplegable: una de las fijas o una del catálogo del centro. */
    static final class Opcion {
        final String tipo;
        final String nombre;      // solo las del catálogo
        final boolean permanente;
        final Integer dias;       // días sugeridos
        final String etiqueta;

        Opcion(String tipo, String nombre, boolean permanente, Integer dias, String etiqueta) {
            this.tipo = tipo;
            this.nombre = nombre;
            this.permanente = permanente;
            this.dias = dias;
            this.etiqueta = etiqueta;
        }

        boolean pideDias() {
            return "CIRUGIA_RECIENTE".equals(tipo) || ("PERSONALIZADA".equals(tipo) && !permanente);
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    /** Arma las opciones: primero las fijas y después las del catálogo que trajo la API. */
    static List<Opcion> opciones(Context contexto, JSONArray catalogo) {
        List<Opcion> opciones = new ArrayList<>();
        opciones.add(new Opcion("TATUAJE", null, false, null, contexto.getString(R.string.restriccion_tatuaje)));
        opciones.add(new Opcion("CIRUGIA_RECIENTE", null, false, null, contexto.getString(R.string.restriccion_cirugia)));
        opciones.add(new Opcion("BAJO_PESO", null, false, null, contexto.getString(R.string.restriccion_bajo_peso)));
        for (int i = 0; i < catalogo.length(); i++) {
            JSONObject t = catalogo.optJSONObject(i);
            if (t == null) {
                continue;
            }
            boolean permanente = t.optBoolean("permanente");
            Integer dias = t.isNull("dias_veda") ? null : t.optInt("dias_veda");
            String veda = permanente ? contexto.getString(R.string.restriccion_permanente)
                    : contexto.getString(R.string.restriccion_dias_veda, dias == null ? 0 : dias);
            opciones.add(new Opcion("PERSONALIZADA", t.optString("nombre"), permanente, dias,
                    t.optString("nombre") + " (" + veda + ")"));
        }
        return opciones;
    }

    /** Muestra el diálogo. Si la restricción ya está en la lista de actuales, no la deja cargar de nuevo. */
    static void mostrar(AppCompatActivity actividad, List<Opcion> opciones, JSONArray actuales, AlElegir alElegir) {
        View contenido = actividad.getLayoutInflater().inflate(R.layout.dialog_restriccion, null);
        final Spinner spinner = contenido.findViewById(R.id.spinnerRestriccion);
        final MaterialButton botonDesde = contenido.findViewById(R.id.botonDesde);
        final View capaDias = contenido.findViewById(R.id.capaDias);
        final TextInputEditText inputDias = contenido.findViewById(R.id.inputDias);
        final TextInputEditText inputObservacion = contenido.findViewById(R.id.inputObservacion);
        final String[] desde = {Fechas.hoy()};
        spinner.setAdapter(new ArrayAdapter<>(actividad, android.R.layout.simple_spinner_dropdown_item, opciones));
        botonDesde.setText(actividad.getString(R.string.restriccion_desde, Fechas.visible(desde[0])));
        botonDesde.setOnClickListener(v -> new DatePickerDialog(actividad, (vista, anio, mes, dia) -> {
            desde[0] = Fechas.armar(anio, mes + 1, dia);
            botonDesde.setText(actividad.getString(R.string.restriccion_desde, Fechas.visible(desde[0])));
        }, Fechas.anio(desde[0]), Fechas.mes(desde[0]) - 1, Fechas.diaDelMes(desde[0])).show());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> padre, View vista, int posicion, long id) {
                Opcion o = opciones.get(posicion);
                capaDias.setVisibility(o.pideDias() ? View.VISIBLE : View.GONE);
                inputDias.setText(o.dias == null ? "" : String.valueOf(o.dias));  // la veda sugerida; el equipo la ajusta
            }

            @Override
            public void onNothingSelected(AdapterView<?> padre) {
            }
        });

        AlertDialog dialogo = new AlertDialog.Builder(actividad)
                .setTitle(R.string.detalle_registrar_restriccion)
                .setView(contenido)
                .setPositiveButton(R.string.restriccion_guardar, null)
                .setNegativeButton(R.string.volver, null)
                .create();
        dialogo.show();
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Opcion o = opciones.get(Math.max(0, spinner.getSelectedItemPosition()));
            if (repetida(actuales, o.tipo, o.nombre)) {
                Toast.makeText(actividad, R.string.restriccion_repetida, Toast.LENGTH_LONG).show();
                return;
            }
            if (desde[0].compareTo(Fechas.hoy()) > 0) {
                Toast.makeText(actividad, R.string.restriccion_fecha_futura, Toast.LENGTH_LONG).show();
                return;
            }
            Integer dias = null;
            if (o.pideDias()) {
                int maximo = "CIRUGIA_RECIENTE".equals(o.tipo) ? 730 : 3650;
                try {
                    dias = Integer.parseInt(texto(inputDias));
                } catch (NumberFormatException e) {
                    dias = null;
                }
                if (dias == null || dias < 1 || dias > maximo) {
                    Toast.makeText(actividad, actividad.getString(R.string.restriccion_dias_invalidos, maximo), Toast.LENGTH_LONG).show();
                    return;
                }
            }
            try {
                JSONObject restriccion = new JSONObject();
                restriccion.put("tipo", o.tipo);
                restriccion.put("desde", desde[0]);
                restriccion.put("dias", dias == null ? JSONObject.NULL : dias);
                restriccion.put("obs", texto(inputObservacion).isEmpty() ? JSONObject.NULL : texto(inputObservacion));
                restriccion.put("nombre", o.nombre == null ? JSONObject.NULL : o.nombre);
                restriccion.put("permanente", o.permanente);
                dialogo.dismiss();
                alElegir.con(restriccion);
            } catch (JSONException e) {
                Toast.makeText(actividad, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Igual criterio que Donante.tieneRestriccion: las fijas no se repiten; las del catálogo, por nombre. */
    static boolean repetida(JSONArray actuales, String tipo, String nombre) {
        for (int i = 0; i < actuales.length(); i++) {
            JSONObject x = actuales.optJSONObject(i);
            if (x == null || !tipo.equals(x.optString("tipo"))) {
                continue;
            }
            if (!"PERSONALIZADA".equals(tipo) || x.optString("nombre").equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }

    private static String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
