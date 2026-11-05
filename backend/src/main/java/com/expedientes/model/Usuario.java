package com.expedientes.model;

public record Usuario(
        Long id,
        String usuario,
        String nombre,
        String passwordHash,
        String rol,
        boolean activo,
        String creadoEn,
        boolean debeCambiarPassword
) {
    public boolean esAdmin() {
        return "ADMIN".equals(rol);
    }
}
