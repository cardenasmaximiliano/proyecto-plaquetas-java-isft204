package com.hemogestion.app.modelo;

import org.json.JSONException;
import org.json.JSONObject;

/** Usuario que ingresó a la app, tal como lo devuelve POST /sesiones. */
public final class Usuario {

    private final long id;
    private final String usuario;
    private final String nombre;
    private final String apellido;
    private final String rol;

    private Usuario(long id, String usuario, String nombre, String apellido, String rol) {
        this.id = id;
        this.usuario = usuario;
        this.nombre = nombre;
        this.apellido = apellido;
        this.rol = rol;
    }

    public static Usuario desdeJson(JSONObject json) throws JSONException {
        return new Usuario(json.getLong("id"), json.getString("usuario"), json.optString("nombre", ""),
                json.optString("apellido", ""), json.getString("rol"));
    }

    public long getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getRol() {
        return rol;
    }

    public String nombreCompleto() {
        return (nombre + " " + apellido).trim();
    }

    /** Nombre del rol para mostrar, como en el escritorio. */
    public static String nombreDelRol(String codigo) {
        switch (codigo) {
            case "SUPERADMIN":
                return "Superadministración";
            case "DIRECTOR":
                return "Director";
            case "EQUIPO_TRABAJO":
                return "Equipo de trabajo";
            default:
                return codigo;
        }
    }
}
