package com.expedientes.controller;

import com.expedientes.config.ApiException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/** Reglas de validación compartidas por los controladores. */
final class Validaciones {

    static final Set<String> STATUS = Set.of("ACTIVO", "INACTIVO");
    // Debe coincidir con SECRETARIAS de frontend/src/api.js
    static final Set<String> SECRETARIAS = Set.of(
            "Organización", "Finanzas", "Asuntos Legales", "Transparencia", "Fomento al Deporte",
            "Fomento al Ahorro", "Pensiones y Jubilaciones", "Oficialía Mayor", "Actas y Acuerdos",
            "De Interior", "Prensa y Propaganda", "Trabajo y Conflictos", "Patrimonio",
            "Fomento a la Vivienda", "Educación y Cultura", "Auditoría", "Acción Social");
    static final Set<String> ROLES = Set.of("ADMIN", "USUARIO");

    private Validaciones() {
    }

    /** Devuelve el texto sin espacios sobrantes o lanza error si viene vacío. */
    static String requerido(String valor, String campo, int maximo) {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.isEmpty()) {
            throw ApiException.invalido("El campo «" + campo + "» es obligatorio.");
        }
        if (limpio.length() > maximo) {
            throw ApiException.invalido("El campo «" + campo + "» admite máximo " + maximo + " caracteres.");
        }
        return limpio;
    }

    static String opcional(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.length() > maximo) {
            throw ApiException.invalido("El texto admite máximo " + maximo + " caracteres.");
        }
        return limpio;
    }

    /** null = sin cambios, "" = quitar, o una imagen como data URL de máximo ~3 MB. */
    static String fotografia(String valor) {
        if (valor == null || valor.isEmpty()) {
            return valor;
        }
        if (!valor.matches("data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/=]+")) {
            throw ApiException.invalido("La fotografía no es una imagen válida.");
        }
        if (valor.length() > 4_000_000) {
            throw ApiException.invalido("La fotografía es demasiado grande.");
        }
        return valor;
    }

    static String status(String valor) {
        String s = valor == null || valor.isBlank() ? "ACTIVO" : valor.trim().toUpperCase();
        if (!STATUS.contains(s)) {
            throw ApiException.invalido("Status no válido.");
        }
        return s;
    }

    static String rol(String valor) {
        String r = valor == null || valor.isBlank() ? "USUARIO" : valor.trim().toUpperCase();
        if (!ROLES.contains(r)) {
            throw ApiException.invalido("Rol no válido.");
        }
        return r;
    }

    static String fecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return LocalDate.now().toString();
        }
        try {
            return LocalDate.parse(valor.trim()).toString();
        } catch (DateTimeParseException e) {
            throw ApiException.invalido("La fecha debe tener el formato AAAA-MM-DD.");
        }
    }

    /** Vacía o una de la lista de SECRETARIAS. */
    static String secretaria(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String s = valor.trim();
        if (!SECRETARIAS.contains(s)) {
            throw ApiException.invalido("Secretaría no válida.");
        }
        return s;
    }

    /** Fecha que puede quedar vacía; no admite fechas futuras (fecha de nacimiento). */
    static String fechaOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String f = fecha(valor);
        if (LocalDate.parse(f).isAfter(LocalDate.now())) {
            throw ApiException.invalido("La fecha de nacimiento no puede ser futura.");
        }
        return f;
    }

    static void password(String valor) {
        if (valor == null || valor.length() < 6) {
            throw ApiException.invalido("La contraseña debe tener al menos 6 caracteres.");
        }
        if (valor.length() > 72) {
            throw ApiException.invalido("La contraseña admite máximo 72 caracteres.");
        }
    }

    static String nombreUsuario(String valor) {
        String u = requerido(valor, "Usuario", 40).toLowerCase();
        if (!u.matches("[a-z0-9._-]+")) {
            throw ApiException.invalido("El usuario solo puede tener letras, números, punto, guion y guion bajo.");
        }
        return u;
    }
}
