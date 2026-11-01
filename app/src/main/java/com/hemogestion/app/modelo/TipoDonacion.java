package com.hemogestion.app.modelo;

/** Tipo de donación del catálogo del centro (SANGRE_ENTERA, PLAQUETAS...). */
public final class TipoDonacion {

    private final String codigo;
    private final String nombre;

    public TipoDonacion(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;  // lo que muestra un Spinner
    }
}
