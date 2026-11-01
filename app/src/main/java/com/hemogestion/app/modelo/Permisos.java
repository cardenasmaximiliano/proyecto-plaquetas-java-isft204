package com.hemogestion.app.modelo;

/**
 * Lo que puede hacer cada rol, para habilitar u ocultar botones. Es la misma tabla que usa la API
 * (apiPHP/src/Nucleo.php) y el escritorio (domain/Permisos.cpp); la API la vuelve a verificar siempre.
 */
public final class Permisos {

    private final String rol;

    public Permisos(String rol) {
        this.rol = rol == null ? "" : rol;
    }

    private boolean esSuperAdmin() {
        return "SUPERADMIN".equals(rol);
    }

    private boolean esDirector() {
        return "DIRECTOR".equals(rol);
    }

    private boolean esEquipo() {
        return "EQUIPO_TRABAJO".equals(rol);
    }

    /** Donantes, datos clínicos, turnos, donaciones y mensajes: los tres roles. */
    public boolean atiende() {
        return esSuperAdmin() || esDirector() || esEquipo();
    }

    /** Definir el resultado de una donación (apto / diferido). */
    public boolean defineResultado() {
        return esSuperAdmin() || esDirector();
    }

    public boolean veInformes() {
        return esSuperAdmin() || esDirector();
    }

    /** Eliminar (borrado lógico) donaciones y otros registros. */
    public boolean eliminaRegistros() {
        return esSuperAdmin() || esDirector();
    }

    public boolean gestionaRestricciones() {
        return esSuperAdmin() || esDirector();
    }

    /** Alta de usuarios: el SuperAdmin crea cualquier rol; el Director, Director o Equipo de trabajo. */
    public boolean creaUsuarios() {
        return esSuperAdmin() || esDirector();
    }

    /** Ver la lista de usuarios del sistema: solo el SuperAdmin (la API lo exige con GestionarUsuarios). */
    public boolean veUsuarios() {
        return esSuperAdmin();
    }
}
