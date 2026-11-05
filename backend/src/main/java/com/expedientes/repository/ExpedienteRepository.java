package com.expedientes.repository;

import com.expedientes.dto.Dtos.ExpedienteRequest;
import com.expedientes.dto.Dtos.Resumen;
import com.expedientes.model.Expediente;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ExpedienteRepository {

    private static final String SELECT = """
            SELECT e.id, e.numero, e.status, e.fecha_ingreso, e.nombre, e.fecha_nacimiento, e.solicitud,
                   e.secretaria, e.perfil, e.telefono, e.familiar_sindicalizado, e.parentesco,
                   e.dependencia_familiar, e.notas, %s AS fotografia,
                   e.creado_por, e.creado_en, e.actualizado_en, u.nombre AS creado_por_nombre
            FROM expedientes e
            LEFT JOIN usuarios u ON u.id = e.creado_por
            """;
    private static final String SELECT_LISTA = SELECT.formatted("NULL");
    private static final String SELECT_COMPLETO = SELECT.formatted("e.fotografia");

    private static final RowMapper<Expediente> MAPPER = (rs, i) -> {
        long creadoPorValor = rs.getLong("creado_por");
        Long creadoPor = rs.wasNull() ? null : creadoPorValor;
        return new Expediente(
                rs.getLong("id"),
                rs.getString("numero"),
                rs.getString("status"),
                rs.getString("fecha_ingreso"),
                rs.getString("nombre"),
                rs.getString("fecha_nacimiento"),
                rs.getString("solicitud"),
                rs.getString("secretaria"),
                rs.getString("perfil"),
                rs.getString("telefono"),
                rs.getString("familiar_sindicalizado"),
                rs.getString("parentesco"),
                rs.getString("dependencia_familiar"),
                rs.getString("notas"),
                rs.getString("fotografia"),
                creadoPor,
                rs.getString("creado_por_nombre"),
                rs.getString("creado_en"),
                rs.getString("actualizado_en")
        );
    };

    private final JdbcTemplate jdbc;

    public ExpedienteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public static final String SIN_SECRETARIA = "sin-asignar";

    public List<Expediente> buscar(String texto, String status, String secretaria) {
        StringBuilder sql = new StringBuilder(SELECT_LISTA).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (texto != null && !texto.isBlank()) {
            sql.append(" AND (e.numero LIKE ? OR e.nombre LIKE ? OR e.solicitud LIKE ? OR e.secretaria LIKE ?"
                    + " OR e.telefono LIKE ?)");
            String patron = "%" + texto.trim() + "%";
            for (int i = 0; i < 5; i++) {
                params.add(patron);
            }
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND e.status = ?");
            params.add(status);
        }
        if (SIN_SECRETARIA.equals(secretaria)) {
            sql.append(" AND e.secretaria IS NULL");
        } else if (secretaria != null && !secretaria.isBlank()) {
            sql.append(" AND e.secretaria = ?");
            params.add(secretaria);
        }
        sql.append(" ORDER BY e.id DESC");
        return jdbc.query(sql.toString(), MAPPER, params.toArray());
    }

    public Optional<Expediente> buscarPorId(long id) {
        return jdbc.query(SELECT_COMPLETO + " WHERE e.id = ?", MAPPER, id).stream().findFirst();
    }

    public boolean numeroOcupado(String numero, long idPropio) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM expedientes WHERE numero = ? AND id <> ?",
                Integer.class, numero, idPropio);
        return n != null && n > 0;
    }

    public long crear(ExpedienteRequest d, long creadoPor) {
        Long id = jdbc.execute((ConnectionCallback<Long>) con -> {
            try (PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO expedientes (numero, status, fecha_ingreso, nombre, fecha_nacimiento, solicitud,
                                             secretaria, perfil, telefono, familiar_sindicalizado, parentesco,
                                             dependencia_familiar, notas, fotografia, creado_por)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""")) {
                ps.setString(1, d.numero());
                ps.setString(2, d.status());
                ps.setString(3, d.fechaIngreso());
                ps.setString(4, d.nombre());
                ps.setString(5, d.fechaNacimiento());
                ps.setString(6, d.solicitud());
                ps.setString(7, d.secretaria());
                ps.setString(8, d.perfil());
                ps.setString(9, d.telefono());
                ps.setString(10, d.familiarSindicalizado());
                ps.setString(11, d.parentesco());
                ps.setString(12, d.dependenciaFamiliar());
                ps.setString(13, d.notas());
                ps.setString(14, d.fotografia() == null || d.fotografia().isEmpty() ? null : d.fotografia());
                ps.setLong(15, creadoPor);
                ps.executeUpdate();
            }
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                return rs.getLong(1);
            }
        });
        return id == null ? 0 : id;
    }

    public void actualizar(long id, ExpedienteRequest d) {
        jdbc.update("""
                        UPDATE expedientes
                        SET numero = ?, status = ?, fecha_ingreso = ?, nombre = ?, fecha_nacimiento = ?,
                            solicitud = ?, secretaria = ?, perfil = ?, telefono = ?,
                            familiar_sindicalizado = ?, parentesco = ?, dependencia_familiar = ?, notas = ?,
                            fotografia = CASE WHEN ? IS NULL THEN fotografia ELSE NULLIF(?, '') END,
                            actualizado_en = datetime('now', 'localtime')
                        WHERE id = ?""",
                d.numero(), d.status(), d.fechaIngreso(), d.nombre(), d.fechaNacimiento(),
                d.solicitud(), d.secretaria(), d.perfil(), d.telefono(),
                d.familiarSindicalizado(), d.parentesco(), d.dependenciaFamiliar(), d.notas(),
                d.fotografia(), d.fotografia(), id);
    }

    public void eliminar(long id) {
        jdbc.update("DELETE FROM expedientes WHERE id = ?", id);
    }

    public Resumen resumen() {
        return jdbc.queryForObject("""
                        SELECT COUNT(*) AS total,
                               COALESCE(SUM(status = 'ACTIVO'), 0)   AS activos,
                               COALESCE(SUM(status = 'INACTIVO'), 0) AS inactivos
                        FROM expedientes""",
                (rs, i) -> new Resumen(rs.getLong("total"), rs.getLong("activos"), rs.getLong("inactivos")));
    }
}
