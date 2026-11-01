package com.hemogestion.app.modelo;

import com.hemogestion.app.util.Texto;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Donante tal como lo devuelve la API.
 *
 * Se guarda el JSON completo: al editar se envían también los campos que la app no muestra
 * (consentimientos, diferimientos, etc.) para que la API no los pise con vacío.
 */
public final class Donante {

    private final JSONObject datos;

    private Donante(JSONObject datos) {
        this.datos = datos;
    }

    public static Donante desdeJson(JSONObject json) {
        return new Donante(json);
    }

    /** Donante nuevo con los valores que espera la API al crear. */
    public static Donante nuevo() throws JSONException {
        JSONObject d = new JSONObject();
        d.put("tipo_documento", "DNI");
        d.put("restricciones", new JSONArray());
        d.put("acepta_email", false);
        d.put("acepta_whatsapp", false);
        d.put("diferido_permanente", false);
        d.put("activo", true);
        d.put("fecha_alta", Texto.ahoraIso());
        return new Donante(d);
    }

    public long getId() {
        return datos.optLong("id", 0);
    }

    public int getVersion() {
        return datos.optInt("version", 1);
    }

    public String texto(String campo) {
        return textoDe(datos, campo);
    }

    /** Un objeto anidado del donante (por ejemplo el informe de laboratorio); null si no tiene. */
    public JSONObject objeto(String campo) {
        return datos.optJSONObject(campo);
    }

    public boolean bool(String campo) {
        return datos.optBoolean(campo, false);
    }

    /** Lista de restricciones: [{"tipo", "desde", "dias", "obs"}]. */
    public JSONArray restricciones() {
        JSONArray r = datos.optJSONArray("restricciones");
        return r != null ? r : new JSONArray();
    }

    /**
     * ¿Ya tiene cargada esa restricción? Las fijas se distinguen por su tipo; las médicas del catálogo, por su nombre
     * (sin importar mayúsculas), igual que valida el escritorio.
     */
    public boolean tieneRestriccion(String tipo, String nombre) {
        JSONArray r = restricciones();
        for (int i = 0; i < r.length(); i++) {
            JSONObject x = r.optJSONObject(i);
            if (x == null || !tipo.equals(x.optString("tipo"))) {
                continue;
            }
            if (!"PERSONALIZADA".equals(tipo) || textoDe(x, "nombre").equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }

    public void agregarRestriccion(JSONObject restriccion) throws JSONException {
        JSONArray r = restricciones();
        r.put(restriccion);
        datos.put("restricciones", r);
    }

    public void quitarRestriccion(int posicion) throws JSONException {
        JSONArray r = restricciones();
        r.remove(posicion);
        datos.put("restricciones", r);
    }

    /** Asigna un campo. Un valor null borra el campo (la API lo guarda como NULL). */
    public void set(String campo, Object valor) throws JSONException {
        datos.put(campo, valor);
    }

    /** JSON completo que se envía a la API. */
    public JSONObject toJson() {
        return datos;
    }

    public String nombreCompleto() {
        return texto("apellido") + ", " + texto("nombre");
    }

    /** "AB+", "0-" o "Sin grupo" si no se cargó. */
    public String grupoFactor() {
        String grupo = texto("grupo_sanguineo");
        if (grupo.isEmpty()) {
            return "Sin grupo";
        }
        return grupo + texto("factor_rh");
    }

    /** Una línea por restricción, para mostrar en la ficha. */
    public List<String> lineasRestricciones() {
        List<String> lineas = new ArrayList<>();
        JSONArray r = restricciones();
        for (int i = 0; i < r.length(); i++) {
            JSONObject x = r.optJSONObject(i);
            if (x != null) {
                lineas.add(describir(x));
            }
        }
        return lineas;
    }

    private static String nombreTipo(String codigo) {
        switch (codigo) {
            case "TATUAJE":
                return "Tatuaje";
            case "CIRUGIA_RECIENTE":
                return "Cirugía reciente";
            case "BAJO_PESO":
                return "Bajo peso";
            case "PERSONALIZADA":
                return "Restricción médica";
            default:
                return codigo;
        }
    }

    /** Texto de una restricción para mostrar: "Embarazo (permanente) desde 2026-10-01 — observación". */
    public static String describir(JSONObject x) {
        String tipo = textoDe(x, "tipo");
        StringBuilder linea = new StringBuilder("PERSONALIZADA".equals(tipo) && !textoDe(x, "nombre").isEmpty() ? textoDe(x, "nombre") : nombreTipo(tipo));
        if (x.optBoolean("permanente", false)) {
            linea.append(" (permanente)");
        } else if (!textoDe(x, "dias").isEmpty()) {
            linea.append(" (").append(textoDe(x, "dias")).append(" días)");
        }
        String desde = textoDe(x, "desde");
        if (!desde.isEmpty()) {
            linea.append(" desde ").append(com.hemogestion.app.util.Fechas.visible(desde));
        }
        String obs = textoDe(x, "obs");
        if (!obs.isEmpty()) {
            linea.append(" — ").append(obs);
        }
        return linea.toString();
    }

    /** Valor de un campo como texto; los null de JSON quedan como "" (org.json de Android devuelve "null"). */
    private static String textoDe(JSONObject o, String campo) {
        Object v = o.opt(campo);
        return v == null || v == JSONObject.NULL ? "" : String.valueOf(v);
    }
}
