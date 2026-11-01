package com.hemogestion.app.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FechasTest {

    @Test
    public void diaDeLaSemanaEmpiezaEnLunes() {
        assertEquals(1, Fechas.diaSemana("2026-10-26"));   // lunes
        assertEquals(5, Fechas.diaSemana("2026-10-30"));   // viernes
        assertEquals(7, Fechas.diaSemana("2026-10-25"));   // domingo
    }

    @Test
    public void elLunesDeLaSemanaDeUnDomingoEsElAnterior() {
        assertEquals("2026-10-19", Fechas.lunesDe("2026-10-25"));
        assertEquals("2026-10-26", Fechas.lunesDe("2026-10-26"));
        assertEquals("2026-10-26", Fechas.lunesDe("2026-10-30"));
    }

    @Test
    public void sumarDiasCruzaMesesYAnios() {
        assertEquals("2026-11-01", Fechas.sumarDias("2026-10-31", 1));
        assertEquals("2026-02-28", Fechas.sumarDias("2026-03-01", -1));
        assertEquals("2027-01-01", Fechas.sumarDias("2026-12-31", 1));
        assertEquals("2026-11-02", Fechas.sumarDias("2026-10-26", 7));
    }

    @Test
    public void etiquetasYFormatoVisible() {
        assertEquals("Lun 26", Fechas.etiquetaDia("2026-10-26"));
        assertEquals("Dom 25", Fechas.etiquetaDia("2026-10-25"));
        assertEquals("26/10/2026", Fechas.visible("2026-10-26"));
        assertEquals("sin fecha", Fechas.visible("sin fecha"));
        assertEquals("", Fechas.visible(null));
    }

    @Test
    public void elDiaDelCentroSeConvierteAUtcConTresHorasDeDiferencia() {
        assertEquals("2026-10-26T03:00:00Z", Fechas.inicioDelDiaUtc("2026-10-26"));
        assertEquals("2026-10-26T13:30:00Z", Fechas.aInstanteUtc("2026-10-26", "10:30"));
    }

    @Test
    public void unInstanteDeLaApiSeMuestraEnLaHoraDelCentro() {
        assertEquals("10:30", Fechas.hora("2026-10-26T13:30:00Z"));
        assertEquals("26/10/2026 10:30", Fechas.fechaHora("2026-10-26T13:30:00Z"));
        assertEquals("2026-10-26", Fechas.dia("2026-10-26T13:30:00Z"));
        // 01:00 UTC del 27 todavía es la noche del 26 en Argentina
        assertEquals("2026-10-26", Fechas.dia("2026-10-27T01:00:00Z"));
        assertEquals("", Fechas.hora("no es una fecha"));
    }

    @Test
    public void armarCompletaConCeros() {
        assertEquals("2026-03-07", Fechas.armar(2026, 3, 7));
        assertEquals(2026, Fechas.anio("2026-03-07"));
        assertEquals(3, Fechas.mes("2026-03-07"));
        assertEquals(7, Fechas.diaDelMes("2026-03-07"));
    }
}
