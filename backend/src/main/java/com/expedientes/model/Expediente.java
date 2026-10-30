package com.expedientes.model;

public record Expediente(
        Long id,
        String numero,
        String status,
        String fechaIngreso,
        String nombre,
        String fechaNacimiento,
        String solicitud,
        String secretaria,
        String perfil,
        String telefono,
        String familiarSindicalizado,
        String parentesco,
        String dependenciaFamiliar,
        String notas,
        String fotografia,
        Long creadoPor,
        String creadoPorNombre,
        String creadoEn,
        String actualizadoEn
) {
}
