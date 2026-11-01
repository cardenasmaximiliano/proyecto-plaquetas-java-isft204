package com.hemogestion.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.hemogestion.app.R;

/** Barra de navegación de abajo: Inicio, Agenda, Búsqueda avanzada y Estadísticas. */
public final class BarraInferior {

    public static final int INICIO = 0;
    public static final int AGENDA = 1;
    public static final int BUSQUEDA = 2;
    public static final int ESTADISTICAS = 3;

    private static final int[] BOTONES = {R.id.navInicio, R.id.navAgenda, R.id.navBusqueda, R.id.navEstadisticas};
    private static final int[] ICONOS = {R.id.navIconoInicio, R.id.navIconoAgenda, R.id.navIconoBusqueda, R.id.navIconoEstadisticas};
    private static final int[] ETIQUETAS = {R.id.navTextoInicio, R.id.navTextoAgenda, R.id.navTextoBusqueda, R.id.navTextoEstadisticas};
    private static final Class<?>[] DESTINOS = {InicioActivity.class, AgendaActivity.class, BusquedaAvanzadaActivity.class, EstadisticasActivity.class};

    private BarraInferior() {
    }

    /** `seleccionado` es la pestaña que se resalta (una de las constantes de esta clase). */
    public static void configurar(Activity actividad, int seleccionado) {
        for (int i = 0; i < BOTONES.length; i++) {
            final int destino = i;
            boolean activo = i == seleccionado;
            View boton = actividad.findViewById(BOTONES[i]);
            boton.setSelected(activo);
            int color = ContextCompat.getColor(actividad, activo ? R.color.hemo_primario : R.color.hemo_texto_secundario);
            ((ImageView) actividad.findViewById(ICONOS[i])).setColorFilter(color);
            TextView etiqueta = actividad.findViewById(ETIQUETAS[i]);
            etiqueta.setTextColor(color);
            boton.setOnClickListener(v -> ir(actividad, seleccionado, destino));
        }
    }

    private static void ir(Activity actividad, int actual, int destino) {
        if (destino == actual) {
            return;
        }
        Intent intent = new Intent(actividad, DESTINOS[destino]);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        actividad.startActivity(intent);
        actividad.overridePendingTransition(0, 0);
        // Las pestañas se reemplazan entre sí: Inicio queda siempre abajo en la pila.
        if (!(actividad instanceof InicioActivity)) {
            actividad.finish();
        }
    }
}
