package com.expedientes.config;

import com.expedientes.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final UsuarioRepository usuarios;
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder;
    private final String adminUsuario;
    private final String adminPassword;

    public DatosIniciales(UsuarioRepository usuarios, JdbcTemplate jdbc, BCryptPasswordEncoder encoder,
                          @Value("${app.admin.usuario}") String adminUsuario,
                          @Value("${app.admin.password}") String adminPassword) {
        this.usuarios = usuarios;
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.adminUsuario = adminUsuario;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        convertirFormatoAnterior();
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_expedientes_status ON expedientes(status)");

        agregarColumnaCambioPassword();
        if (usuarios.contar() == 0) {
            usuarios.crear(adminUsuario, adminUsuario, encoder.encode(adminPassword), "ADMIN");
            log.warn("Se creó el usuario '{}' con la contraseña inicial. Cámbiala al entrar.", adminUsuario);
        }
        exigirCambioDePasswordInicial();
    }

    private void exigirCambioDePasswordInicial() {
        usuarios.listar().stream()
                .filter(u -> encoder.matches(adminPassword, u.passwordHash()))
                .forEach(u -> usuarios.exigirCambioPassword(u.id()));
    }

    private void agregarColumnaCambioPassword() {
        Integer columna = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pragma_table_info('usuarios') WHERE name = 'debe_cambiar_password'", Integer.class);
        if (columna == null || columna == 0) {
            jdbc.execute("ALTER TABLE usuarios ADD COLUMN debe_cambiar_password INTEGER NOT NULL DEFAULT 0");
        }
    }

    private void convertirFormatoAnterior() {
        Integer anterior = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pragma_table_info('expedientes') WHERE name = 'titular'", Integer.class);
        if (anterior == null || anterior == 0) {
            return;
        }
        Integer conFoto = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pragma_table_info('expedientes') WHERE name = 'fotografia'", Integer.class);
        String foto = conFoto != null && conFoto > 0 ? "fotografia" : "NULL";

        jdbc.execute("DROP INDEX IF EXISTS idx_expedientes_estado");
        jdbc.execute("ALTER TABLE expedientes RENAME TO expedientes_anterior");
        jdbc.execute("""
                CREATE TABLE expedientes (
                    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
                    numero                  TEXT    UNIQUE,
                    status                  TEXT    NOT NULL DEFAULT 'ACTIVO' CHECK (status IN ('ACTIVO', 'INACTIVO')),
                    fecha_ingreso           TEXT    NOT NULL,
                    nombre                  TEXT    NOT NULL,
                    fecha_nacimiento        TEXT,
                    solicitud               TEXT,
                    secretaria              TEXT,
                    perfil                  TEXT,
                    telefono                TEXT,
                    familiar_sindicalizado  TEXT,
                    parentesco              TEXT,
                    dependencia_familiar    TEXT,
                    notas                   TEXT,
                    fotografia              TEXT,
                    creado_por              INTEGER REFERENCES usuarios(id),
                    creado_en               TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
                    actualizado_en          TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
                )""");
        int copiados = jdbc.update("""
                INSERT INTO expedientes (id, numero, status, fecha_ingreso, nombre, solicitud, notas, fotografia,
                                         creado_por, creado_en, actualizado_en)
                SELECT id, folio, CASE WHEN estado = 'CERRADO' THEN 'INACTIVO' ELSE 'ACTIVO' END,
                       fecha_apertura, titular, asunto, descripcion, %s, creado_por, creado_en, actualizado_en
                FROM expedientes_anterior""".formatted(foto));
        jdbc.execute("DROP TABLE expedientes_anterior");
        log.info("Se convirtieron {} expedientes al formato nuevo.", copiados);
    }
}
