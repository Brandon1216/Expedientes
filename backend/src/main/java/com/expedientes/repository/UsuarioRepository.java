package com.expedientes.repository;

import com.expedientes.model.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioRepository {

    private static final RowMapper<Usuario> MAPPER = (rs, i) -> new Usuario(
            rs.getLong("id"),
            rs.getString("usuario"),
            rs.getString("nombre"),
            rs.getString("password_hash"),
            rs.getString("rol"),
            rs.getInt("activo") == 1,
            rs.getString("creado_en")
    );

    private final JdbcTemplate jdbc;

    public UsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Usuario> listar() {
        return jdbc.query("SELECT * FROM usuarios ORDER BY rol, nombre COLLATE NOCASE", MAPPER);
    }

    public Optional<Usuario> buscarPorId(long id) {
        return jdbc.query("SELECT * FROM usuarios WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public Optional<Usuario> buscarPorUsuario(String usuario) {
        return jdbc.query("SELECT * FROM usuarios WHERE usuario = ? COLLATE NOCASE", MAPPER, usuario)
                .stream().findFirst();
    }

    public long contar() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Long.class);
        return n == null ? 0 : n;
    }

    public long contarAdminsActivos() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE rol = 'ADMIN' AND activo = 1", Long.class);
        return n == null ? 0 : n;
    }

    public void crear(String usuario, String nombre, String passwordHash, String rol) {
        jdbc.update("INSERT INTO usuarios (usuario, nombre, password_hash, rol) VALUES (?, ?, ?, ?)",
                usuario, nombre, passwordHash, rol);
    }

    public void actualizar(long id, String nombre, String rol, boolean activo) {
        jdbc.update("UPDATE usuarios SET nombre = ?, rol = ?, activo = ? WHERE id = ?",
                nombre, rol, activo ? 1 : 0, id);
    }

    public void cambiarPassword(long id, String passwordHash) {
        jdbc.update("UPDATE usuarios SET password_hash = ? WHERE id = ?", passwordHash, id);
    }

    public void eliminar(long id) {
        // Los expedientes que creó se conservan, solo pierden la referencia al autor
        jdbc.update("UPDATE expedientes SET creado_por = NULL WHERE creado_por = ?", id);
        jdbc.update("DELETE FROM usuarios WHERE id = ?", id);
    }
}
