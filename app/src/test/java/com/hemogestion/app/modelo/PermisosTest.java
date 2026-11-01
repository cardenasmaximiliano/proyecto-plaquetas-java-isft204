package com.hemogestion.app.modelo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PermisosTest {

    @Test
    public void elEquipoDeTrabajoAtiendePeroNoDefineResultadosNiVeInformes() {
        Permisos equipo = new Permisos("EQUIPO_TRABAJO");
        assertTrue(equipo.atiende());
        assertFalse(equipo.defineResultado());
        assertFalse(equipo.veInformes());
        assertFalse(equipo.gestionaRestricciones());
    }

    @Test
    public void elDirectorYElSuperAdminPuedenTodoLoDeLaApp() {
        for (String rol : new String[] {"DIRECTOR", "SUPERADMIN"}) {
            Permisos p = new Permisos(rol);
            assertTrue(rol, p.atiende());
            assertTrue(rol, p.defineResultado());
            assertTrue(rol, p.veInformes());
            assertTrue(rol, p.gestionaRestricciones());
        }
    }

    @Test
    public void soloLaDireccionEliminaRegistros() {
        assertFalse(new Permisos("EQUIPO_TRABAJO").eliminaRegistros());
        assertTrue(new Permisos("DIRECTOR").eliminaRegistros());
        assertTrue(new Permisos("SUPERADMIN").eliminaRegistros());
        assertFalse(new Permisos(null).eliminaRegistros());
    }

    @Test
    public void unRolDesconocidoNoPuedeNada() {
        assertFalse(new Permisos("OTRO").atiende());
        assertFalse(new Permisos(null).atiende());
    }

    @Test
    public void nombresVisiblesDeLosRoles() {
        assertEquals("Superadministración", Usuario.nombreDelRol("SUPERADMIN"));
        assertEquals("Director", Usuario.nombreDelRol("DIRECTOR"));
        assertEquals("Equipo de trabajo", Usuario.nombreDelRol("EQUIPO_TRABAJO"));
    }
}
