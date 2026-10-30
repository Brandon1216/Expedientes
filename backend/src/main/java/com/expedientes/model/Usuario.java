package com.expedientes.model;

/** Usuario del sistema. passwordHash nunca se envía al frontend. */
public record Usuario(
        Long id,
        String usuario,
        String nombre,
        String passwordHash,
        String rol,
        boolean activo,
        String creadoEn
) {
    public boolean esAdmin() {
        return "ADMIN".equals(rol);
    }
}
