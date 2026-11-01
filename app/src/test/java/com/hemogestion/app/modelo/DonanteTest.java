package com.hemogestion.app.modelo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class DonanteTest {

    private static Donante donanteConRestricciones(String json) throws JSONException {
        JSONObject d = new JSONObject();
        d.put("id", 7);
        d.put("apellido", "Gómez");
        d.put("nombre", "María");
        d.put("grupo_sanguineo", "AB");
        d.put("factor_rh", "+");
        d.put("restricciones", new JSONArray(json));
        return Donante.desdeJson(d);
    }

    @Test
    public void unDonanteNuevoTieneLosValoresQueEsperaLaApi() throws JSONException {
        JSONObject json = Donante.nuevo().toJson();
        assertEquals("DNI", json.getString("tipo_documento"));
        assertEquals(0, json.getJSONArray("restricciones").length());
        assertFalse(json.getBoolean("acepta_whatsapp"));
        assertTrue(json.getBoolean("activo"));
        assertTrue(json.getString("fecha_alta").endsWith("Z"));
    }

    @Test
    public void losNullDeJsonSeLeenComoTextoVacio() throws JSONException {
        Donante d = Donante.nuevo();
        d.set("email", JSONObject.NULL);
        assertEquals("", d.texto("email"));      // el org.json de Android devolvería "null"
        assertEquals("", d.texto("no_existe"));
    }

    @Test
    public void nombreYGrupoParaMostrar() throws JSONException {
        Donante d = donanteConRestricciones("[]");
        assertEquals("Gómez, María", d.nombreCompleto());
        assertEquals("AB+", d.grupoFactor());
        assertEquals("Sin grupo", Donante.nuevo().grupoFactor());
        assertEquals(7, d.getId());
    }

    @Test
    public void lasRestriccionesSeDescribenConSuNombreYSusDias() throws JSONException {
        Donante d = donanteConRestricciones("["
                + "{\"tipo\":\"TATUAJE\",\"desde\":\"2026-08-01\",\"dias\":null,\"obs\":null},"
                + "{\"tipo\":\"PERSONALIZADA\",\"desde\":\"2026-10-01\",\"dias\":300,\"obs\":\"Semana 20\",\"nombre\":\"Embarazo\",\"permanente\":false},"
                + "{\"tipo\":\"PERSONALIZADA\",\"desde\":\"2026-09-01\",\"dias\":null,\"obs\":null,\"nombre\":\"Cardiopatía\",\"permanente\":true}]");
        assertEquals(3, d.lineasRestricciones().size());
        assertEquals("Tatuaje desde 01/08/2026", d.lineasRestricciones().get(0));
        assertEquals("Embarazo (300 días) desde 01/10/2026 — Semana 20", d.lineasRestricciones().get(1));
        assertEquals("Cardiopatía (permanente) desde 01/09/2026", d.lineasRestricciones().get(2));
    }

    @Test
    public void unaRestriccionRepetidaSeDetectaPorTipoOPorNombre() throws JSONException {
        Donante d = donanteConRestricciones("["
                + "{\"tipo\":\"TATUAJE\",\"desde\":\"2026-08-01\"},"
                + "{\"tipo\":\"PERSONALIZADA\",\"desde\":\"2026-10-01\",\"dias\":300,\"nombre\":\"Embarazo\"}]");
        assertTrue(d.tieneRestriccion("TATUAJE", null));
        assertFalse(d.tieneRestriccion("BAJO_PESO", null));
        assertTrue(d.tieneRestriccion("PERSONALIZADA", "embarazo"));       // sin importar mayúsculas
        assertFalse(d.tieneRestriccion("PERSONALIZADA", "Cardiopatía"));
    }

    @Test
    public void agregarYQuitarRestriccionesModificanElJsonQueSeEnvia() throws JSONException {
        Donante d = donanteConRestricciones("[]");
        d.agregarRestriccion(new JSONObject().put("tipo", "BAJO_PESO").put("desde", "2026-10-01"));
        d.agregarRestriccion(new JSONObject().put("tipo", "TATUAJE").put("desde", "2026-08-01"));
        assertEquals(2, d.toJson().getJSONArray("restricciones").length());
        d.quitarRestriccion(0);
        assertEquals(1, d.restricciones().length());
        assertEquals("TATUAJE", d.restricciones().getJSONObject(0).getString("tipo"));
    }
}
