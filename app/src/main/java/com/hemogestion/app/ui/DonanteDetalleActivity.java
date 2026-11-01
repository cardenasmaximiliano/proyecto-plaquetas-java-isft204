package com.hemogestion.app.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hemogestion.app.R;
import com.hemogestion.app.modelo.Donante;
import com.hemogestion.app.modelo.TipoDonacion;
import com.hemogestion.app.util.Fechas;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Ficha del donante: datos, restricciones, historial de donaciones y accesos para agendar o registrar una donación. */
public class DonanteDetalleActivity extends BaseActivity {

    public static final String EXTRA_ID = "donante_id";

    /** Lo que se trae de la API para armar la pantalla. */
    private static final class Datos {
        Donante donante;
        JSONArray donaciones;
        JSONArray externas;
    }

    // Precisión de la fecha de un historial externo, en el orden de strings/arrays.xml (precision_nombres).
    private static final String[] PRECISIONES = {"DIA", "MES", "ANIO"};

    private long donanteId;
    private Donante donante;
    private TextView textoFicha;
    private LinearLayout listaRestricciones;
    private LinearLayout bloqueInforme;

    // Campos del informe de laboratorio en el orden en que se muestran, con su etiqueta (la misma del formulario).
    private static final String[] CLAVES_INFORME = {"hemoglobina", "hematocrito", "globulos_rojos", "vcm", "hcm", "chcm",
            "globulos_blancos", "neutrofilos", "linfocitos", "monocitos", "eosinofilos", "basofilos", "plaquetas"};
    private static final int[] ETIQUETAS_INFORME = {R.string.informe_hemoglobina, R.string.informe_hematocrito,
            R.string.informe_globulos_rojos, R.string.informe_vcm, R.string.informe_hcm, R.string.informe_chcm,
            R.string.informe_globulos_blancos, R.string.informe_neutrofilos, R.string.informe_linfocitos,
            R.string.informe_monocitos, R.string.informe_eosinofilos, R.string.informe_basofilos, R.string.informe_plaquetas};
    private LinearLayout listaDonaciones;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        if (!haySesion()) {
            return;
        }
        setContentView(R.layout.activity_donante_detalle);
        donanteId = getIntent().getLongExtra(EXTRA_ID, 0);
        textoFicha = findViewById(R.id.textoFicha);
        listaRestricciones = findViewById(R.id.listaRestricciones);
        bloqueInforme = findViewById(R.id.bloqueInforme);
        listaDonaciones = findViewById(R.id.listaDonaciones);

        findViewById(R.id.botonVolver).setOnClickListener(v -> finish());
        findViewById(R.id.botonEditar).setOnClickListener(v -> {
            Intent intent = new Intent(this, DonanteFormActivity.class);
            intent.putExtra(DonanteFormActivity.EXTRA_ID, donanteId);
            startActivity(intent);
        });
        findViewById(R.id.botonRegistrarDonacion).setOnClickListener(v -> {
            Intent intent = new Intent(this, DonacionFormActivity.class);
            intent.putExtra(DonacionFormActivity.EXTRA_DONANTE_ID, donanteId);
            intent.putExtra(DonacionFormActivity.EXTRA_DONANTE_NOMBRE, donante == null ? null : donante.nombreCompleto());
            startActivity(intent);
        });
        findViewById(R.id.botonAgendarTurno).setOnClickListener(v -> {
            Intent intent = new Intent(this, AgendarActivity.class);
            intent.putExtra(AgendarActivity.EXTRA_DONANTE_ID, donanteId);
            intent.putExtra(AgendarActivity.EXTRA_DONANTE_NOMBRE, donante == null ? null : donante.nombreCompleto());
            startActivity(intent);
        });
        findViewById(R.id.botonRegistrarRestriccion).setOnClickListener(v -> pedirRestriccion());
        findViewById(R.id.botonLevantarEspera).setOnClickListener(v -> pedirLevantarEspera());
        findViewById(R.id.botonHistorialExterno).setOnClickListener(v -> pedirHistorialExterno());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (haySesion()) {
            recargar();
        }
    }

    private void recargar() {
        cargar(() -> {
            Datos d = new Datos();
            d.donante = api.obtenerDonante(donanteId);
            d.donaciones = api.lista("/donaciones?donante_id=" + donanteId);
            d.externas = api.lista("/historial-externo?donante_id=" + donanteId);
            api.tiposDonacion();
            return d;
        }, d -> {
            donante = d.donante;
            mostrarFicha();
            mostrarRestricciones();
            mostrarInforme();
            mostrarHistorial(d.donaciones, d.externas);
        });
    }

    // ---- Ficha ------------------------------------------------------------------------------------------------

    private static String valor(String texto) {
        return texto.isEmpty() ? "—" : texto;
    }

    private void mostrarFicha() {
        StringBuilder ficha = new StringBuilder();
        ficha.append("DNI: ").append(donante.texto("numero_documento")).append('\n');
        ficha.append("Nombre: ").append(donante.nombreCompleto()).append('\n');
        ficha.append("Nacimiento: ").append(valor(Fechas.visible(donante.texto("fecha_nacimiento")))).append('\n');
        ficha.append("Sexo: ").append("F".equals(donante.texto("sexo")) ? "Femenino" : "Masculino").append('\n');
        ficha.append("Grupo sanguíneo: ").append(donante.grupoFactor()).append('\n');
        ficha.append("Peso: ").append(valor(donante.texto("peso_kg"))).append(" kg\n");
        ficha.append("Teléfono: ").append(valor(donante.texto("telefono_e164"))).append('\n');
        ficha.append("Correo: ").append(valor(donante.texto("email"))).append('\n');
        ficha.append("Domicilio: ").append(valor(donante.texto("domicilio"))).append('\n');
        ficha.append("Localidad: ").append(valor(donante.texto("localidad"))).append('\n');
        ficha.append("Recordatorios: WhatsApp ").append(donante.bool("acepta_whatsapp") ? "sí" : "no")
                .append(" · correo ").append(donante.bool("acepta_email") ? "sí" : "no");
        if (!donante.texto("observaciones").isEmpty()) {
            ficha.append('\n').append("Observaciones: ").append(donante.texto("observaciones"));
        }
        textoFicha.setText(ficha.toString());
    }

    // ---- Restricciones ----------------------------------------------------------------------------------------

    /** Informe de laboratorio del donante: solo los valores cargados; si no hay ninguno, lo dice. */
    private void mostrarInforme() {
        bloqueInforme.removeAllViews();
        JSONObject informe = donante.objeto("informe_laboratorio");
        int cargados = 0;
        if (informe != null) {
            String fecha = informe.optString("fecha_informe", "");
            if (!fecha.isEmpty()) {
                agregarLineaInforme(getString(R.string.detalle_informe_fecha, Fechas.visible(fecha)));
                cargados++;
            }
            for (int i = 0; i < CLAVES_INFORME.length; i++) {
                if (!informe.isNull(CLAVES_INFORME[i])) {
                    agregarLineaInforme(getString(ETIQUETAS_INFORME[i]) + ": " + numero(informe.optDouble(CLAVES_INFORME[i])));
                    cargados++;
                }
            }
        }
        if (cargados == 0) {
            agregarLineaInforme(getString(R.string.detalle_sin_informe));
        }
    }

    private void agregarLineaInforme(String texto) {
        TextView linea = new TextView(this);
        linea.setText(texto);
        linea.setPadding(0, 8, 0, 8);
        bloqueInforme.addView(linea);
    }

    private void mostrarRestricciones() {
        listaRestricciones.removeAllViews();
        JSONArray restricciones = donante.restricciones();
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
            final String descripcion = Donante.describir(r);
            ((TextView) fila.findViewById(R.id.textoRestriccion)).setText(descripcion);
            fila.findViewById(R.id.botonQuitar).setOnClickListener(v -> confirmarQuitar(posicion, descripcion));
            listaRestricciones.addView(fila);
        }
    }

    private void confirmarQuitar(int posicion, String descripcion) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.restriccion_quitar_confirmar, descripcion))
                .setPositiveButton(R.string.quitar, (d, w) -> {
                    try {
                        donante.quitarRestriccion(posicion);
                    } catch (JSONException e) {
                        mostrarError(e.getMessage());
                        return;
                    }
                    guardarDonante(getString(R.string.restriccion_quitada));
                })
                .setNegativeButton(R.string.volver, null)
                .show();
    }

    private void guardarDonante(String mensajeListo) {
        cargar(() -> api.actualizarDonante(donante), version -> {
            aviso(mensajeListo);
            recargar();
        }, this::recargar);
    }

    private void pedirRestriccion() {
        if (donante == null) {
            return;
        }
        cargar(() -> api.lista("/configuracion/tipos-restriccion"), catalogo ->
                DialogoRestriccion.mostrar(this, DialogoRestriccion.opciones(this, catalogo), donante.restricciones(), restriccion -> {
                    try {
                        donante.agregarRestriccion(restriccion);
                        guardarDonante(getString(R.string.restriccion_guardada));
                    } catch (JSONException e) {
                        mostrarError(e.getMessage());
                    }
                }));
    }

    // ---- Historial --------------------------------------------------------------------------------------------

    /** Signos vitales de una donación para el historial: solo los que se cargaron, como en el escritorio. */
    private String textoVitales(JSONObject d) {
        List<String> partes = new ArrayList<>();
        if (!d.isNull("pa_antes_sistolica") && !d.isNull("pa_antes_diastolica")) {
            partes.add(getString(R.string.vitales_presion, d.optInt("pa_antes_sistolica"), d.optInt("pa_antes_diastolica")));
        }
        if (!d.isNull("frecuencia_cardiaca")) {
            partes.add(getString(R.string.vitales_pulso, d.optInt("frecuencia_cardiaca")));
        }
        if (!d.isNull("temperatura_c")) {
            partes.add(getString(R.string.vitales_temperatura, numero(d.optDouble("temperatura_c"))));
        }
        if (!d.isNull("peso_dia_kg")) {
            partes.add(getString(R.string.vitales_peso, numero(d.optDouble("peso_dia_kg"))));
        }
        return TextUtils.join(" · ", partes);
    }

    /** 70.0 se muestra como 70; 36.5 como 36.5. */
    private static String numero(double valor) {
        return valor == Math.rint(valor) ? String.valueOf((long) valor) : String.valueOf(valor);
    }

    private void mostrarHistorial(JSONArray donaciones, JSONArray externas) {
        listaDonaciones.removeAllViews();
        for (int i = 0; i < donaciones.length(); i++) {
            JSONObject d = donaciones.optJSONObject(i);
            if (d == null) {
                continue;
            }
            String resultado = d.optString("resultado");
            String tipo = api.nombreDelTipo(d.optString("tipo_donacion"));
            if (!d.optBoolean("se_completo", true)) {
                tipo += " " + getString(R.string.historial_sin_extraccion);
            }
            String vitales = textoVitales(d);
            if (!vitales.isEmpty()) {
                tipo += "\n" + vitales;
            }
            View fila = crearFilaHistorial(Fechas.visible(Fechas.dia(d.optString("fecha_hora"))), tipo,
                    nombreDelResultado(resultado), fondoDelResultado(resultado), colorDelResultado(resultado));
            fila.setOnClickListener(v -> mostrarAccionesDonacion(d));
            listaDonaciones.addView(fila);
        }
        for (int i = 0; i < externas.length(); i++) {
            JSONObject e = externas.optJSONObject(i);
            if (e == null) {
                continue;
            }
            String tipo = api.nombreDelTipo(e.optString("tipo_donacion")) + " × " + e.optInt("cantidad", 1);
            View fila = crearFilaHistorial(Fechas.visible(e.optString("fecha_aproximada")), tipo,
                    getString(R.string.historial_externa), R.drawable.bg_chip_neutro, R.color.hemo_texto_secundario);
            fila.setOnClickListener(v -> mostrarAccionesExterno(e));
            listaDonaciones.addView(fila);
        }
        if (listaDonaciones.getChildCount() == 0) {
            TextView vacio = new TextView(this);
            vacio.setText(R.string.historial_vacio);
            listaDonaciones.addView(vacio);
        }
    }

    // ---- Acciones sobre una donación ---------------------------------------------------------------------------

    private void mostrarAccionesDonacion(JSONObject d) {
        final String titulo = getString(R.string.donacion_acciones, Fechas.visible(Fechas.dia(d.optString("fecha_hora"))));
        List<String> textos = new ArrayList<>();
        List<Runnable> acciones = new ArrayList<>();
        textos.add(getString(R.string.donacion_accion_corregir));
        acciones.add(() -> {
            Intent intent = new Intent(this, DonacionFormActivity.class);
            intent.putExtra(DonacionFormActivity.EXTRA_DONANTE_ID, donanteId);
            intent.putExtra(DonacionFormActivity.EXTRA_DONANTE_NOMBRE, donante == null ? null : donante.nombreCompleto());
            intent.putExtra(DonacionFormActivity.EXTRA_DONACION, d.toString());
            startActivity(intent);
        });
        textos.add(getString(R.string.donacion_accion_recuperacion));
        acciones.add(() -> pedirRecuperacion(d));
        if (permisos.eliminaRegistros()) {
            textos.add(getString(R.string.donacion_accion_eliminar));
            acciones.add(() -> confirmarEliminarDonacion(d));
        }
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setItems(textos.toArray(new String[0]), (dialogo, posicion) -> acciones.get(posicion).run())
                .show();
    }

    private void pedirRecuperacion(JSONObject d) {
        final long id = d.optLong("id");
        View contenido = getLayoutInflater().inflate(R.layout.dialog_recuperacion, null);
        final CheckBox quitar = contenido.findViewById(R.id.checkQuitarAjuste);
        final MaterialButton botonFecha = contenido.findViewById(R.id.botonPuedeDonarDesde);
        final TextInputEditText motivo = contenido.findViewById(R.id.inputMotivo);
        final String actual = d.isNull("puede_donar_desde") ? null : d.optString("puede_donar_desde");
        ((TextView) contenido.findViewById(R.id.textoActual)).setText(actual == null ? getString(R.string.recuperacion_sin_ajuste)
                : getString(R.string.recuperacion_actual, Fechas.visible(actual)));
        quitar.setEnabled(actual != null);
        final String[] fecha = {actual != null ? actual : Fechas.hoy()};
        botonFecha.setText(getString(R.string.recuperacion_fecha, Fechas.visible(fecha[0])));
        botonFecha.setOnClickListener(v -> new DatePickerDialog(this, (vista, anio, mes, dia) -> {
            fecha[0] = Fechas.armar(anio, mes + 1, dia);
            botonFecha.setText(getString(R.string.recuperacion_fecha, Fechas.visible(fecha[0])));
        }, Fechas.anio(fecha[0]), Fechas.mes(fecha[0]) - 1, Fechas.diaDelMes(fecha[0])).show());
        quitar.setOnCheckedChangeListener((vista, marcado) -> botonFecha.setEnabled(!marcado));

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(R.string.recuperacion_titulo)
                .setView(contenido)
                .setPositiveButton(R.string.recuperacion_guardar, null)
                .setNegativeButton(R.string.volver, null)
                .create();
        dialogo.show();
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (texto(motivo).isEmpty()) {
                motivo.setError(getString(R.string.espera_sin_motivo));
                return;
            }
            JSONObject cuerpo = new JSONObject();
            try {
                cuerpo.put("puede_donar_desde", quitar.isChecked() ? JSONObject.NULL : fecha[0]);
                cuerpo.put("motivo", texto(motivo));
            } catch (JSONException e) {
                mostrarError(e.getMessage());
                return;
            }
            dialogo.dismiss();
            cargar(() -> api.objeto("POST", "/donaciones/" + id + "/recuperacion", cuerpo), r -> {
                aviso(getString(R.string.recuperacion_guardada));
                recargar();
            });
        });
    }

    private void confirmarEliminarDonacion(JSONObject d) {
        final long id = d.optLong("id");
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.donacion_eliminar_confirmar, Fechas.visible(Fechas.dia(d.optString("fecha_hora")))))
                .setPositiveButton(R.string.donacion_accion_eliminar, (dialogo, w) ->
                        cargar(() -> api.objeto("POST", "/donaciones/" + id + "/eliminar", new JSONObject()), r -> {
                            aviso(getString(R.string.donacion_eliminada));
                            recargar();
                        }))
                .setNegativeButton(R.string.volver, null)
                .show();
    }

    // ---- Levantar la espera ------------------------------------------------------------------------------------

    private void pedirLevantarEspera() {
        View contenido = getLayoutInflater().inflate(R.layout.dialog_motivo, null);
        ((TextView) contenido.findViewById(R.id.textoExplicacion)).setText(R.string.espera_explicacion);
        final TextInputEditText motivo = contenido.findViewById(R.id.inputMotivo);
        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(R.string.espera_titulo)
                .setView(contenido)
                .setPositiveButton(R.string.espera_confirmar, null)
                .setNegativeButton(R.string.volver, null)
                .create();
        dialogo.show();
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (texto(motivo).isEmpty()) {
                motivo.setError(getString(R.string.espera_sin_motivo));
                return;
            }
            JSONObject cuerpo = new JSONObject();
            try {
                cuerpo.put("motivo", texto(motivo));
            } catch (JSONException e) {
                mostrarError(e.getMessage());
                return;
            }
            dialogo.dismiss();
            cargar(() -> api.objeto("POST", "/donantes/" + donanteId + "/levantar-espera", cuerpo), r -> {
                aviso(getString(R.string.espera_listo, r.optInt("levantadas")));
                recargar();
            });
        });
    }

    // ---- Historial externo -------------------------------------------------------------------------------------

    private void pedirHistorialExterno() {
        cargar(() -> api.tiposDonacion(), this::mostrarDialogoExterno);
    }

    private void mostrarDialogoExterno(List<TipoDonacion> tipos) {
        View contenido = getLayoutInflater().inflate(R.layout.dialog_historial_externo, null);
        final Spinner spinnerTipo = contenido.findViewById(R.id.spinnerTipoExterno);
        final Spinner spinnerPrecision = contenido.findViewById(R.id.spinnerPrecision);
        final TextInputEditText inputCantidad = contenido.findViewById(R.id.inputCantidad);
        final TextInputEditText inputLugar = contenido.findViewById(R.id.inputLugar);
        final TextInputEditText inputObservacion = contenido.findViewById(R.id.inputObservacionExterna);
        final MaterialButton botonFecha = contenido.findViewById(R.id.botonFechaExterna);
        spinnerTipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tipos));
        spinnerPrecision.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.precision_nombres)));
        final String[] fecha = {Fechas.hoy()};
        botonFecha.setText(getString(R.string.externo_fecha, Fechas.visible(fecha[0])));
        botonFecha.setOnClickListener(v -> new DatePickerDialog(this, (vista, anio, mes, dia) -> {
            fecha[0] = Fechas.armar(anio, mes + 1, dia);
            botonFecha.setText(getString(R.string.externo_fecha, Fechas.visible(fecha[0])));
        }, Fechas.anio(fecha[0]), Fechas.mes(fecha[0]) - 1, Fechas.diaDelMes(fecha[0])).show());

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(R.string.externo_titulo)
                .setView(contenido)
                .setPositiveButton(R.string.externo_guardar, null)
                .setNegativeButton(R.string.volver, null)
                .create();
        dialogo.show();
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int cantidad;
            try {
                cantidad = Integer.parseInt(texto(inputCantidad));
            } catch (NumberFormatException e) {
                cantidad = 0;
            }
            if (cantidad < 1 || cantidad > 200) {
                mostrarError(getString(R.string.externo_cantidad_invalida));
                return;
            }
            if (fecha[0].compareTo(Fechas.hoy()) > 0) {
                mostrarError(getString(R.string.externo_fecha_futura));
                return;
            }
            JSONObject cuerpo = new JSONObject();
            try {
                cuerpo.put("donante_id", donanteId);
                cuerpo.put("tipo_donacion", ((TipoDonacion) spinnerTipo.getSelectedItem()).getCodigo());
                cuerpo.put("cantidad", cantidad);
                cuerpo.put("precision_fecha", PRECISIONES[Math.max(0, spinnerPrecision.getSelectedItemPosition())]);
                cuerpo.put("fecha_aproximada", fecha[0]);
                cuerpo.put("lugar", texto(inputLugar));
                cuerpo.put("observaciones", texto(inputObservacion));
            } catch (JSONException e) {
                mostrarError(e.getMessage());
                return;
            }
            dialogo.dismiss();
            cargar(() -> api.objeto("POST", "/historial-externo/registrar", cuerpo), r -> {
                aviso(getString(R.string.externo_guardado));
                recargar();
            });
        });
    }

    private void mostrarAccionesExterno(JSONObject e) {
        final long id = e.optLong("id");
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.externo_acciones, Fechas.visible(e.optString("fecha_aproximada"))))
                .setItems(new String[] {getString(R.string.externo_eliminar)}, (dialogo, posicion) ->
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.externo_eliminar_confirmar)
                                .setPositiveButton(R.string.externo_eliminar, (d2, w) ->
                                        cargar(() -> api.objeto("POST", "/historial-externo/" + id + "/eliminar", new JSONObject()), r -> {
                                            aviso(getString(R.string.externo_eliminado));
                                            recargar();
                                        }))
                                .setNegativeButton(R.string.volver, null)
                                .show())
                .show();
    }

    private View crearFilaHistorial(String fecha, String tipo, String chip, int fondo, int color) {
        View fila = getLayoutInflater().inflate(R.layout.item_donacion, listaDonaciones, false);
        ((TextView) fila.findViewById(R.id.textoFechaDonacion)).setText(fecha);
        ((TextView) fila.findViewById(R.id.textoTipoDonacion)).setText(tipo);
        TextView etiqueta = fila.findViewById(R.id.textoResultadoDonacion);
        etiqueta.setText(chip);
        etiqueta.setBackgroundResource(fondo);
        etiqueta.setTextColor(ContextCompat.getColor(this, color));
        return fila;
    }

    private static String nombreDelResultado(String resultado) {
        switch (resultado) {
            case "APTO":
                return "Apto";
            case "DIFERIDO_TEMPORAL":
                return "Diferido";
            case "DIFERIDO_PERMANENTE":
                return "Diferido permanente";
            default:
                return "Pendiente";
        }
    }

    private static int fondoDelResultado(String resultado) {
        switch (resultado) {
            case "APTO":
                return R.drawable.bg_chip_exito;
            case "DIFERIDO_TEMPORAL":
                return R.drawable.bg_chip_alerta;
            case "DIFERIDO_PERMANENTE":
                return R.drawable.bg_chip_error;
            default:
                return R.drawable.bg_chip_neutro;
        }
    }

    private static int colorDelResultado(String resultado) {
        switch (resultado) {
            case "APTO":
                return R.color.hemo_exito;
            case "DIFERIDO_TEMPORAL":
                return R.color.hemo_advertencia;
            case "DIFERIDO_PERMANENTE":
                return R.color.hemo_error;
            default:
                return R.color.hemo_texto_secundario;
        }
    }
}
