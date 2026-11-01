package com.hemogestion.app.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Fechas de la app. Los "días" son los del centro (la API guarda en UTC y calcula la elegibilidad con esta
 * misma zona), así que no dependen de la zona horaria que tenga configurada el celular.
 */
public final class Fechas {

    public static final TimeZone ZONA = TimeZone.getTimeZone("America/Argentina/Buenos_Aires");
    private static final String[] DIAS = {"", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private Fechas() {
    }

    private static SimpleDateFormat formato(String patron, TimeZone zona) {
        SimpleDateFormat f = new SimpleDateFormat(patron, Locale.US);
        f.setTimeZone(zona);
        f.setLenient(false);
        return f;
    }

    private static Date deIso(String iso) {
        try {
            return formato("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC")).parse(iso);
        } catch (ParseException | NullPointerException e) {
            return null;
        }
    }

    /** Instante ISO de la API ("2026-10-26T13:00:00Z") -> "10:00" en la hora del centro. */
    public static String hora(String iso) {
        Date d = deIso(iso);
        return d == null ? "" : formato("HH:mm", ZONA).format(d);
    }

    /** Instante ISO de la API -> "26/10/2026 10:00". */
    public static String fechaHora(String iso) {
        Date d = deIso(iso);
        return d == null ? "" : formato("dd/MM/yyyy HH:mm", ZONA).format(d);
    }

    /** Instante ISO de la API -> día del centro "2026-10-26". */
    public static String dia(String iso) {
        Date d = deIso(iso);
        return d == null ? "" : formato("yyyy-MM-dd", ZONA).format(d);
    }

    /** "2026-10-26" -> "26/10/2026"; si no tiene ese formato, lo devuelve igual. */
    public static String visible(String ymd) {
        if (ymd == null || !ymd.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return ymd == null ? "" : ymd;
        }
        return ymd.substring(8, 10) + "/" + ymd.substring(5, 7) + "/" + ymd.substring(0, 4);
    }

    public static String hoy() {
        return formato("yyyy-MM-dd", ZONA).format(new Date());
    }

    /** Ahora, en la hora del centro, como "HH:mm". */
    public static String horaActual() {
        return formato("HH:mm", ZONA).format(new Date());
    }

    private static Calendar calendario(String ymd) {
        Calendar c = Calendar.getInstance(ZONA, Locale.US);
        c.clear();
        c.set(Integer.parseInt(ymd.substring(0, 4)), Integer.parseInt(ymd.substring(5, 7)) - 1, Integer.parseInt(ymd.substring(8, 10)));
        return c;
    }

    public static String sumarDias(String ymd, int dias) {
        Calendar c = calendario(ymd);
        c.add(Calendar.DAY_OF_MONTH, dias);
        return formato("yyyy-MM-dd", ZONA).format(c.getTime());
    }

    /** Inicio de ese día del centro, como instante ISO en UTC (para consultar la API). */
    public static String inicioDelDiaUtc(String ymd) {
        return formato("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC")).format(calendario(ymd).getTime());
    }

    /** 1 = lunes ... 7 = domingo. */
    public static int diaSemana(String ymd) {
        int d = calendario(ymd).get(Calendar.DAY_OF_WEEK);  // 1 = domingo
        return d == Calendar.SUNDAY ? 7 : d - 1;
    }

    /** El lunes de la semana de ese día. */
    public static String lunesDe(String ymd) {
        return sumarDias(ymd, -(diaSemana(ymd) - 1));
    }

    /** "Lun 26". */
    public static String etiquetaDia(String ymd) {
        return DIAS[diaSemana(ymd)] + " " + Integer.parseInt(ymd.substring(8, 10));
    }

    public static int anio(String ymd) {
        return Integer.parseInt(ymd.substring(0, 4));
    }

    public static int mes(String ymd) {
        return Integer.parseInt(ymd.substring(5, 7));
    }

    public static int diaDelMes(String ymd) {
        return Integer.parseInt(ymd.substring(8, 10));
    }

    public static String armar(int anio, int mes, int dia) {
        return String.format(Locale.US, "%04d-%02d-%02d", anio, mes, dia);
    }

    /** Fecha y hora del centro ("2026-10-26", "10:30") -> instante ISO en UTC. */
    public static String aInstanteUtc(String ymd, String hhmm) {
        Calendar c = calendario(ymd);
        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hhmm.substring(0, 2)));
        c.set(Calendar.MINUTE, Integer.parseInt(hhmm.substring(3, 5)));
        return formato("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC")).format(c.getTime());
    }
}
