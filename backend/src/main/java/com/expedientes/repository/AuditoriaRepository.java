package com.expedientes.repository;

import com.expedientes.dto.Dtos.Movimiento;
import com.expedientes.dto.Dtos.Pagina;
import com.expedientes.dto.Dtos.ResumenAuditoria;
import com.expedientes.dto.Dtos.SesionRegistro;
import com.expedientes.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditoriaRepository {

    public record FiltroMovimientos(String usuario, String accion, String texto, String desde, String hasta,
                                    boolean ocultarSesiones) {
    }

    public record FiltroSesiones(String usuario, String texto, String desde, String hasta) {
    }

    private static final String ACCIONES_EXPEDIENTE = "('CREAR_EXPEDIENTE', 'EDITAR_EXPEDIENTE', 'ELIMINAR_EXPEDIENTE')";

    private static final String SELECT_MOVIMIENTOS = """
            SELECT a.id, a.fecha, a.usuario, a.nombre, a.equipo, a.accion, a.objeto, a.detalle, a.cambios,
                   a.expediente_id,
                   EXISTS (SELECT 1 FROM expedientes e WHERE e.id = a.expediente_id) AS expediente_existe,
                   (SELECT s.equipo_nombre FROM sesiones s
                    WHERE s.equipo = a.equipo AND s.equipo_nombre IS NOT NULL ORDER BY s.id DESC LIMIT 1) AS equipo_nombre
            FROM auditoria a""";

    private static final String SELECT_SESIONES = """
            SELECT s.id, s.usuario, s.nombre, s.equipo, s.equipo_nombre, s.inicio, s.ultima_actividad, s.fin,
                   s.motivo_fin,
                   CASE WHEN s.fin IS NOT NULL THEN 'CERRADA'
                        WHEN julianday('now', 'localtime') - julianday(s.inicio) > ? / 24.0 THEN 'EXPIRADA'
                        ELSE 'ABIERTA' END AS estado,
                   EXISTS (SELECT 1 FROM sesiones o
                           WHERE o.usuario_id = s.usuario_id AND o.id <> s.id
                             AND o.inicio < COALESCE(s.fin, CASE
                                     WHEN julianday('now', 'localtime') - julianday(s.inicio) > ? / 24.0
                                     THEN s.ultima_actividad ELSE '9999-12-31 23:59:59' END)
                             AND COALESCE(o.fin, CASE
                                     WHEN julianday('now', 'localtime') - julianday(o.inicio) > ? / 24.0
                                     THEN o.ultima_actividad ELSE '9999-12-31 23:59:59' END) > s.inicio)
                       AS simultanea,
                   (EXISTS (SELECT 1 FROM sesiones o WHERE o.usuario_id = s.usuario_id AND o.id < s.id)
                    AND NOT EXISTS (SELECT 1 FROM sesiones o
                                    WHERE o.usuario_id = s.usuario_id AND o.id < s.id AND o.equipo = s.equipo))
                       AS equipo_nuevo,
                   (strftime('%w', s.inicio) = '0'
                    OR CAST(strftime('%H', s.inicio) AS INTEGER) < ?
                    OR CAST(strftime('%H', s.inicio) AS INTEGER) >= ?) AS fuera_horario
            FROM sesiones s""";

    private final JdbcTemplate jdbc;
    private final long horasSesion;
    private final int horaEntrada;
    private final int horaSalida;

    public AuditoriaRepository(JdbcTemplate jdbc,
                               @Value("${app.token.horas}") long horasSesion,
                               @Value("${app.horario.entrada:7}") int horaEntrada,
                               @Value("${app.horario.salida:20}") int horaSalida) {
        this.jdbc = jdbc;
        this.horasSesion = horasSesion;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
    }

    public long abrirSesion(Usuario u, String equipo) {
        Long id = jdbc.execute((ConnectionCallback<Long>) con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO sesiones (usuario_id, usuario, nombre, equipo) VALUES (?, ?, ?, ?)")) {
                ps.setLong(1, u.id());
                ps.setString(2, u.usuario());
                ps.setString(3, u.nombre());
                ps.setString(4, equipo);
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

    public void nombrarEquipo(long sesion, String nombre) {
        jdbc.update("UPDATE sesiones SET equipo_nombre = ? WHERE id = ?", nombre, sesion);
    }

    public boolean sesionAbierta(long id, long usuarioId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sesiones WHERE id = ? AND usuario_id = ? AND fin IS NULL",
                Integer.class, id, usuarioId);
        return n != null && n > 0;
    }

    public void registrarActividad(long id) {
        jdbc.update("""
                UPDATE sesiones SET ultima_actividad = datetime('now', 'localtime')
                WHERE id = ? AND ultima_actividad < datetime('now', 'localtime', '-1 minute')""", id);
    }

    public boolean cerrarSesion(long id, String motivo) {
        return jdbc.update("""
                UPDATE sesiones SET fin = datetime('now', 'localtime'), motivo_fin = ?
                WHERE id = ? AND fin IS NULL""", motivo, id) > 0;
    }

    public void cerrarSesionesDe(long usuarioId, String motivo) {
        jdbc.update("""
                UPDATE sesiones SET fin = datetime('now', 'localtime'), motivo_fin = ?
                WHERE usuario_id = ? AND fin IS NULL""", motivo, usuarioId);
    }

    public void registrar(Usuario quien, String equipo, String accion, String objeto, String detalle) {
        registrar(quien.id(), quien.usuario(), quien.nombre(), equipo, accion, objeto, detalle);
    }

    public void registrar(Long usuarioId, String usuario, String nombre, String equipo, String accion,
                          String objeto, String detalle) {
        jdbc.update("""
                INSERT INTO auditoria (usuario_id, usuario, nombre, equipo, accion, objeto, detalle)
                VALUES (?, ?, ?, ?, ?, ?, ?)""", usuarioId, usuario, nombre, equipo, accion, objeto, detalle);
    }

    public void registrarExpediente(Usuario quien, String equipo, String accion, long expedienteId, String objeto,
                                    String detalle, String cambios) {
        jdbc.update("""
                        INSERT INTO auditoria (usuario_id, usuario, nombre, equipo, accion, expediente_id, objeto,
                                               detalle, cambios)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                quien.id(), quien.usuario(), quien.nombre(), equipo, accion, expedienteId, objeto, detalle, cambios);
    }

    public Pagina<Movimiento> movimientos(FiltroMovimientos f, long desde, int tamano) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();
        condiciones(f, true, where, params);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM auditoria a" + where, Long.class, params.toArray());

        List<Object> conPagina = new ArrayList<>(params);
        conPagina.add(tamano);
        conPagina.add(desde);
        List<Movimiento> filas = jdbc.query(SELECT_MOVIMIENTOS + where + " ORDER BY a.id DESC LIMIT ? OFFSET ?",
                (rs, i) -> {
                    long expediente = rs.getLong("expediente_id");
                    Long expedienteId = rs.wasNull() ? null : expediente;
                    return new Movimiento(rs.getLong("id"), rs.getString("fecha"), rs.getString("usuario"),
                            rs.getString("nombre"), rs.getString("equipo"), rs.getString("equipo_nombre"),
                            rs.getString("accion"), expedienteId, rs.getInt("expediente_existe") == 1,
                            rs.getString("objeto"), rs.getString("detalle"), rs.getString("cambios"));
                }, conPagina.toArray());
        return new Pagina<>(total == null ? 0 : total, filas);
    }

    public ResumenAuditoria resumen(FiltroMovimientos f) {
        StringBuilder conTodo = new StringBuilder();
        List<Object> paramsTodo = new ArrayList<>();
        condiciones(f, true, conTodo, paramsTodo);
        Long movimientos = jdbc.queryForObject("SELECT COUNT(*) FROM auditoria a" + conTodo, Long.class,
                paramsTodo.toArray());

        StringBuilder rango = new StringBuilder();
        List<Object> paramsRango = new ArrayList<>();
        condiciones(f, false, rango, paramsRango);
        return jdbc.queryForObject("""
                        SELECT COUNT(DISTINCT CASE WHEN a.accion IN %s
                                    THEN COALESCE(CAST(a.expediente_id AS TEXT), a.objeto) END) AS expedientes,
                               COALESCE(SUM(a.accion = 'ELIMINAR_EXPEDIENTE'), 0) AS eliminaciones,
                               COUNT(DISTINCT CASE WHEN a.accion <> 'INICIO_FALLIDO' THEN a.usuario END) AS usuarios
                        FROM auditoria a""".formatted(ACCIONES_EXPEDIENTE) + rango,
                (rs, i) -> new ResumenAuditoria(movimientos == null ? 0 : movimientos, rs.getLong("expedientes"),
                        rs.getLong("eliminaciones"), rs.getLong("usuarios")),
                paramsRango.toArray());
    }

    private static void condiciones(FiltroMovimientos f, boolean conAccion, StringBuilder sql, List<Object> params) {
        sql.append(" WHERE 1 = 1");
        if (vale(f.usuario())) {
            sql.append(" AND a.usuario = ? COLLATE NOCASE");
            params.add(f.usuario());
        }
        if (conAccion && vale(f.accion())) {
            sql.append(" AND a.accion = ?");
            params.add(f.accion());
        }
        if (conAccion && f.ocultarSesiones()) {
            sql.append(" AND a.accion NOT IN ('INICIO_SESION', 'CIERRE_SESION')");
        }
        if (vale(f.texto())) {
            sql.append(" AND (a.objeto LIKE ? OR a.usuario LIKE ? OR a.nombre LIKE ? OR a.detalle LIKE ?)");
            String patron = "%" + f.texto().trim() + "%";
            for (int i = 0; i < 4; i++) {
                params.add(patron);
            }
        }
        fechas(sql, params, "a.fecha", f.desde(), f.hasta());
    }

    public Pagina<SesionRegistro> sesiones(FiltroSesiones f, long desde, int tamano) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (vale(f.usuario())) {
            where.append(" AND s.usuario = ? COLLATE NOCASE");
            params.add(f.usuario());
        }
        if (vale(f.texto())) {
            where.append(" AND (s.usuario LIKE ? OR s.nombre LIKE ? OR s.equipo LIKE ? OR s.equipo_nombre LIKE ?)");
            String patron = "%" + f.texto().trim() + "%";
            for (int i = 0; i < 4; i++) {
                params.add(patron);
            }
        }
        fechas(where, params, "s.inicio", f.desde(), f.hasta());
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM sesiones s" + where, Long.class, params.toArray());

        List<Object> todos = new ArrayList<>();
        todos.add(horasSesion);
        todos.add(horasSesion);
        todos.add(horasSesion);
        todos.add(horaEntrada);
        todos.add(horaSalida);
        todos.addAll(params);
        todos.add(tamano);
        todos.add(desde);
        List<SesionRegistro> filas = jdbc.query(SELECT_SESIONES + where + " ORDER BY s.id DESC LIMIT ? OFFSET ?",
                (rs, i) -> new SesionRegistro(rs.getLong("id"), rs.getString("usuario"), rs.getString("nombre"),
                        rs.getString("equipo"), rs.getString("equipo_nombre"), rs.getString("inicio"),
                        rs.getString("ultima_actividad"), rs.getString("fin"), rs.getString("motivo_fin"),
                        rs.getString("estado"), rs.getInt("simultanea") == 1, rs.getInt("equipo_nuevo") == 1,
                        rs.getInt("fuera_horario") == 1),
                todos.toArray());
        return new Pagina<>(total == null ? 0 : total, filas);
    }

    private static void fechas(StringBuilder sql, List<Object> params, String columna, String desde, String hasta) {
        if (desde != null) {
            sql.append(" AND ").append(columna).append(" >= ?");
            params.add(desde);
        }
        if (hasta != null) {
            sql.append(" AND ").append(columna).append(" < date(?, '+1 day')");
            params.add(hasta);
        }
    }

    private static boolean vale(String valor) {
        return valor != null && !valor.isBlank();
    }

    public List<String> usuarios() {
        return jdbc.queryForList("""
                SELECT usuario FROM sesiones UNION SELECT usuario FROM auditoria
                ORDER BY 1 COLLATE NOCASE""", String.class);
    }
}
