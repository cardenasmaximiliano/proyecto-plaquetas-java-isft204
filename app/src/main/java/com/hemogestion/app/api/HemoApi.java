package com.hemogestion.app.api;

import android.content.Context;

import com.hemogestion.app.BuildConfig;
import com.hemogestion.app.modelo.Donante;
import com.hemogestion.app.modelo.TipoDonacion;
import com.hemogestion.app.modelo.Usuario;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class HemoApi {

    private static final int TIMEOUT_MS = 15_000;

    private static volatile List<TipoDonacion> tiposEnMemoria;

    private final Sesion sesion;

    public HemoApi(Context contexto) {
        sesion = new Sesion(contexto);
    }

    public Sesion sesion() {
        return sesion;
    }

    public Usuario login(String usuario, String password) throws ApiException, JSONException {
        JSONObject cuerpo = new JSONObject().put("usuario", usuario).put("password", password);
        JSONObject respuesta = new JSONObject(pedir("POST", "/sesiones", cuerpo.toString(), false));
        Usuario u = Usuario.desdeJson(respuesta.getJSONObject("usuario"));
        sesion.guardar(respuesta.getString("token"), u.nombreCompleto(), u.getRol());
        return u;
    }

    public void cerrarSesion() {
        try {
            pedir("DELETE", "/sesiones", null, true);
        } catch (ApiException ignorado) {
            // Si no hay conexión, igual se borra el token de este celular.
        }
        sesion.cerrar();
    }

    public JSONObject buscarDonantes(String texto, int offset, int limite) throws ApiException, JSONException {
        String t = texto.trim();
        StringBuilder ruta = new StringBuilder("/donantes/buscar?limite=").append(limite).append("&offset=").append(offset);
        if (!t.isEmpty()) {
            if (t.matches("\\d+")) {
                ruta.append("&modo=numero&digitos=").append(codificar(t));
            } else {
                ruta.append("&modo=nombre&nombre=").append(codificar(normalizarBusqueda(t)));
            }
        }
        return new JSONObject(pedir("GET", ruta.toString(), null, true));
    }

    public Donante obtenerDonante(long id) throws ApiException, JSONException {
        return Donante.desdeJson(new JSONObject(pedir("GET", "/donantes/" + id, null, true)));
    }

    /*crea donante y devuelve el id*/
    public long crearDonante(Donante donante) throws ApiException, JSONException {
        return new JSONObject(pedir("POST", "/donantes", donante.toJson().toString(), true)).getLong("id");
    }

    public int actualizarDonante(Donante donante) throws ApiException, JSONException {
        String ruta = "/donantes/" + donante.getId();
        return new JSONObject(pedir("PUT", ruta, donante.toJson().toString(), true)).getInt("version");
    }


  /*crud para el json*/
    public JSONObject objeto(String metodo, String ruta, JSONObject cuerpo) throws ApiException, JSONException {
        return new JSONObject(pedir(metodo, ruta, cuerpo == null ? null : cuerpo.toString(), true));
    }


    public JSONArray lista(String ruta) throws ApiException, JSONException {
        return new JSONArray(pedir("GET", ruta, null, true));
    }


    public void borrar(String ruta) throws ApiException {
        pedir("DELETE", ruta, null, true);
    }


    public static String param(String valor) {
        return codificar(valor);
    }

   public synchronized List<TipoDonacion> tiposDonacion() throws ApiException, JSONException {
        if (tiposEnMemoria == null) {
            List<TipoDonacion> tipos = new ArrayList<>();
            JSONArray r = lista("/configuracion/tipos-donacion");
            for (int i = 0; i < r.length(); i++) {
                JSONObject t = r.getJSONObject(i);
                if (t.optBoolean("activo", true)) {
                    tipos.add(new TipoDonacion(t.getString("codigo"), t.getString("nombre")));
                }
            }
            tiposEnMemoria = tipos;
        }
        return tiposEnMemoria;
    }


    public String nombreDelTipo(String codigo) {
        if (tiposEnMemoria != null) {
            for (TipoDonacion t : tiposEnMemoria) {
                if (t.getCodigo().equals(codigo)) {
                    return t.getNombre();
                }
            }
        }
        return codigo;
    }

    private String pedir(String metodo, String ruta, String cuerpoJson, boolean conToken) throws ApiException {
        HttpURLConnection conexion = null;
        try {
            conexion = (HttpURLConnection) new URL(BuildConfig.API_BASE_URL + ruta).openConnection();
            conexion.setRequestMethod(metodo);
            conexion.setConnectTimeout(TIMEOUT_MS);
            conexion.setReadTimeout(TIMEOUT_MS);
            conexion.setRequestProperty("Accept", "application/json");
            if (conToken && sesion.token() != null) {
                conexion.setRequestProperty("Authorization", "Bearer " + sesion.token());
            }
            if (cuerpoJson != null) {
                conexion.setDoOutput(true);
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream salida = conexion.getOutputStream()) {
                    salida.write(cuerpoJson.getBytes(StandardCharsets.UTF_8));
                }
            }
            int estado = conexion.getResponseCode();
            InputStream entrada = estado >= 400 ? conexion.getErrorStream() : conexion.getInputStream();
            String texto = leer(entrada);
            if (estado == 401 && conToken) {
                sesion.cerrar();  // el token venció: obliga a ingresar de nuevo
            }
            if (estado >= 400) {
                throw ApiException.desdeRespuesta(estado, texto);
            }
            return texto;
        } catch (IOException e) {
            throw new ApiException(0, ApiException.SIN_CONEXION, e.getMessage());
        } finally {
            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    private static String leer(InputStream entrada) throws IOException {
        if (entrada == null) {
            return "";
        }
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            StringBuilder texto = new StringBuilder();
            String linea;
            while ((linea = lector.readLine()) != null) {
                texto.append(linea).append('\n');
            }
            return texto.toString();
        }
    }

    private static String codificar(String valor) {
        try {
            return URLEncoder.encode(valor, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return valor;  // UTF-8 siempre existe en Android
        }
    }

    private static String normalizarBusqueda(String texto) {
        return com.hemogestion.app.util.Texto.normalizar(texto);
    }
}
