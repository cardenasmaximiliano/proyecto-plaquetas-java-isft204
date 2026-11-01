package com.hemogestion.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TextoTest {

    @Test
    public void normalizarQuitaTildesMayusculasYEspaciosSobrantes() {
        assertEquals("cardenas jose", Texto.normalizar("  Cárdenas   José "));
        assertEquals("nandu perez", Texto.normalizar("ÑANDÚ Pérez"));
        assertEquals("", Texto.normalizar("   "));
    }

    @Test
    public void fechasConElFormatoDeLaApi() {
        assertTrue(Texto.esFecha("2026-10-26"));
        assertFalse(Texto.esFecha("26/10/2026"));
        assertFalse(Texto.esFecha("2026-1-5"));
        assertFalse(Texto.esFecha(null));
    }

    @Test
    public void telefonoVacioOConCodigoDePais() {
        assertTrue(Texto.esTelefono(""));
        assertTrue(Texto.esTelefono("+5492235123456"));
        assertFalse(Texto.esTelefono("2235123456"));
        assertFalse(Texto.esTelefono("+0123456789"));
        assertFalse(Texto.esTelefono("+54 9 223"));
    }

    @Test
    public void elInstanteActualTieneElFormatoDeLaApi() {
        assertTrue(Texto.ahoraIso().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }
}
