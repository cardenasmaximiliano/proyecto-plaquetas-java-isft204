package com.hemogestion.app.util;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Formatos que espera la API: fechas AAAA-MM-DD, instantes ISO en UTC y texto sin tildes para buscar. */
public final class Texto {

    private Texto() {
    }

    /** Instante actual en UTC con formato 2026-10-25T14:00:00Z. */
    public static String ahoraIso() {
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        formato.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formato.format(new Date());
    }

    /** Minúsculas, sin tildes y con espacios simples: el formato de busqueda_normalizada. */
    public static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    public static boolean esFecha(String texto) {
        return texto != null && texto.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    /** Vacío (opcional) o con + y código de país, como +5491123456789. */
    public static boolean esTelefono(String texto) {
        return texto.isEmpty() || texto.matches("\\+[1-9]\\d{7,14}");
    }
}
