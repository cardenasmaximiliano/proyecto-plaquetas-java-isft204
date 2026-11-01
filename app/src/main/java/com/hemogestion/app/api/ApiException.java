package com.hemogestion.app.api;

import org.json.JSONException;
import org.json.JSONObject;

/** Error al hablar con la API: de red, de sesión o un error que devolvió la API. */
public class ApiException extends Exception {

    /** Código propio de la app cuando no hay conexión con la API (no viene de la API). */
    public static final String SIN_CONEXION = "SIN_CONEXION";
    /** Código que devuelve la API cuando el token falta o venció. */
    public static final String SESION_VENCIDA = "NO_AUTENTICADO";

    private final int estado;
    private final String codigo;

    public ApiException(int estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    /** Código HTTP (0 si no hubo respuesta). */
    public int getEstado() {
        return estado;
    }

    public String getCodigo() {
        return codigo;
    }

    /** Lee {"error": {"codigo": "...", "mensaje": "..."}} que devuelve la API. */
    static ApiException desdeRespuesta(int estado, String cuerpo) {
        String codigo = "ERROR";
        String mensaje = "Error de la API (" + estado + ")";
        try {
            JSONObject error = new JSONObject(cuerpo).getJSONObject("error");
            codigo = error.optString("codigo", codigo);
            mensaje = error.optString("mensaje", mensaje);
        } catch (JSONException ignorado) {
            // La respuesta no es JSON: queda el mensaje genérico.
        }
        return new ApiException(estado, codigo, mensaje);
    }
}
