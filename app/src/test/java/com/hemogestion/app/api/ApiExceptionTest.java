package com.hemogestion.app.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ApiExceptionTest {

    @Test
    public void leeElCodigoYElMensajeQueDevuelveLaApi() {
        ApiException e = ApiException.desdeRespuesta(409,
                "{\"error\":{\"codigo\":\"CONFLICTO\",\"mensaje\":\"No quedan cupos libres en ese horario\"}}");
        assertEquals(409, e.getEstado());
        assertEquals("CONFLICTO", e.getCodigo());
        assertEquals("No quedan cupos libres en ese horario", e.getMessage());
    }

    @Test
    public void unaRespuestaQueNoEsJsonDejaUnMensajeGenerico() {
        ApiException e = ApiException.desdeRespuesta(502, "<html>Bad gateway</html>");
        assertEquals("ERROR", e.getCodigo());
        assertEquals("Error de la API (502)", e.getMessage());
    }

    @Test
    public void laSesionVencidaSeReconocePorSuCodigo() {
        ApiException e = ApiException.desdeRespuesta(401, "{\"error\":{\"codigo\":\"NO_AUTENTICADO\",\"mensaje\":\"Falta el token\"}}");
        assertEquals(ApiException.SESION_VENCIDA, e.getCodigo());
    }
}
