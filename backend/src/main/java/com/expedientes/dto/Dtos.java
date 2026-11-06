package com.expedientes.dto;

import com.expedientes.model.Usuario;

public final class Dtos {

    private Dtos() {
    }

    public record LoginRequest(String usuario, String password) {
    }

    public record LoginResponse(String token, UsuarioDto usuario) {
    }

    public record CambiarPasswordRequest(String actual, String nueva) {
    }

    public record UsuarioDto(Long id, String usuario, String nombre, String rol, boolean activo, String creadoEn,
                             boolean debeCambiarPassword) {
        public static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.id(), u.usuario(), u.nombre(), u.rol(), u.activo(), u.creadoEn(),
                    u.debeCambiarPassword());
        }
    }

    public record UsuarioRequest(String usuario, String nombre, String password, String rol, Boolean activo) {
    }

    public record ExpedienteRequest(String numero, String status, String fechaIngreso, String nombre,
                                    String fechaNacimiento, String solicitud, String secretaria, String perfil,
                                    String telefono, String familiarSindicalizado, String parentesco,
                                    String dependenciaFamiliar, String notas, String fotografia,
                                    String actualizadoEn) {
    }

    public record Resumen(long total, long activos, long inactivos) {
    }

    public record ErrorResponse(String error) {
    }

    public record UnidadUsb(String id, String nombre, boolean disponible, long libre) {
    }

    public record CopiaHecha(String fecha, String unidad, String archivo, long bytes, String usuario) {
    }

    public record CopiarUsbRequest(String unidad) {
    }

    public record EstadoRespaldos(java.util.List<UnidadUsb> unidades, java.util.List<CopiaHecha> historial) {
    }

    public record AvisoRespaldo(Integer dias, boolean avisar) {
    }

    public record Movimiento(long id, String fecha, String usuario, String nombre, String equipo,
                             String equipoNombre, String accion, Long expedienteId, boolean expedienteExiste,
                             String objeto, String detalle, String cambios) {
    }

    public record SesionRegistro(long id, String usuario, String nombre, String equipo, String equipoNombre,
                                 String inicio, String ultimaActividad, String fin, String motivoFin, String estado,
                                 boolean simultanea, boolean equipoNuevo, boolean fueraDeHorario) {
    }

    public record Pagina<T>(long total, java.util.List<T> filas) {
    }

    public record ResumenAuditoria(long movimientos, long expedientes, long eliminaciones, long usuarios) {
    }

    public record Cambio(String campo, String antes, String despues) {
    }
}
