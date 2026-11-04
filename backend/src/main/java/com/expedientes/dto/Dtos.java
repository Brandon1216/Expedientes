package com.expedientes.dto;

import com.expedientes.model.Usuario;

/** Objetos que viajan entre el frontend y el backend. */
public final class Dtos {

    private Dtos() {
    }

    public record LoginRequest(String usuario, String password) {
    }

    public record LoginResponse(String token, UsuarioDto usuario) {
    }

    public record CambiarPasswordRequest(String actual, String nueva) {
    }

    /** Usuario sin la contraseña, seguro para enviar al frontend. */
    public record UsuarioDto(Long id, String usuario, String nombre, String rol, boolean activo, String creadoEn,
                             boolean debeCambiarPassword) {
        public static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.id(), u.usuario(), u.nombre(), u.rol(), u.activo(), u.creadoEn(),
                    u.debeCambiarPassword());
        }
    }

    public record UsuarioRequest(String usuario, String nombre, String password, String rol, Boolean activo) {
    }

    /** fotografia: null = no cambiarla, "" = quitarla, data URL = la nueva imagen. */
    public record ExpedienteRequest(String numero, String status, String fechaIngreso, String nombre,
                                    String fechaNacimiento, String solicitud, String secretaria, String perfil,
                                    String telefono, String familiarSindicalizado, String parentesco,
                                    String dependenciaFamiliar, String notas, String fotografia) {
    }

    public record Resumen(long total, long activos, long inactivos) {
    }

    public record ErrorResponse(String error) {
    }

    /** id: letra de la unidad (E:). disponible = false si no se puede leer (cifrada y sin desbloquear). */
    public record UnidadUsb(String id, String nombre, boolean disponible, long libre) {
    }

    public record CopiaHecha(String fecha, String unidad, String archivo, long bytes, String usuario) {
    }

    public record CopiarUsbRequest(String unidad) {
    }

    public record EstadoRespaldos(java.util.List<UnidadUsb> unidades, java.util.List<CopiaHecha> historial) {
    }

    /** dias: desde la última copia a USB, o null si nunca se ha hecho. */
    public record AvisoRespaldo(Integer dias, boolean avisar) {
    }
}
