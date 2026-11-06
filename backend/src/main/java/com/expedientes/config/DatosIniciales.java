package com.expedientes.config;

import com.expedientes.dto.Dtos.Cambio;
import com.expedientes.repository.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final UsuarioRepository usuarios;
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder;
    private final ObjectMapper json;
    private final String adminUsuario;
    private final String adminPassword;

    public DatosIniciales(UsuarioRepository usuarios, JdbcTemplate jdbc, BCryptPasswordEncoder encoder,
                          ObjectMapper json,
                          @Value("${app.admin.usuario}") String adminUsuario,
                          @Value("${app.admin.password}") String adminPassword) {
        this.usuarios = usuarios;
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.json = json;
        this.adminUsuario = adminUsuario;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        convertirFormatoAnterior();
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_expedientes_status ON expedientes(status)");

        agregarColumnaCambioPassword();
        actualizarAuditoria();
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

    private boolean tieneColumna(String tabla, String columna) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pragma_table_info(?) WHERE name = ?",
                Integer.class, tabla, columna);
        return n != null && n > 0;
    }

    private void actualizarAuditoria() {
        if (!tieneColumna("auditoria", "expediente_id")) {
            jdbc.execute("ALTER TABLE auditoria ADD COLUMN expediente_id INTEGER");
        }
        if (!tieneColumna("auditoria", "cambios")) {
            jdbc.execute("ALTER TABLE auditoria ADD COLUMN cambios TEXT");
        }
        if (!tieneColumna("sesiones", "equipo_nombre")) {
            jdbc.execute("ALTER TABLE sesiones ADD COLUMN equipo_nombre TEXT");
        }

        int enlazados = jdbc.update("""
                UPDATE auditoria SET expediente_id = (
                    SELECT e.id FROM expedientes e
                    WHERE e.numero = substr(auditoria.objeto, 1, instr(auditoria.objeto, ' · ') - 1)
                      AND e.creado_en <= auditoria.fecha)
                WHERE expediente_id IS NULL
                  AND accion IN ('CREAR_EXPEDIENTE', 'EDITAR_EXPEDIENTE')
                  AND instr(objeto, ' · ') > 0""");

        List<Map<String, Object>> ediciones = jdbc.queryForList(
                "SELECT id, detalle FROM auditoria WHERE accion = 'EDITAR_EXPEDIENTE' AND cambios IS NULL");
        for (Map<String, Object> fila : ediciones) {
            try {
                String cambios = json.writeValueAsString(leerCambios((String) fila.get("detalle")));
                jdbc.update("UPDATE auditoria SET cambios = ? WHERE id = ?", cambios, fila.get("id"));
            } catch (JsonProcessingException e) {
                log.warn("No se pudo convertir el registro de auditoría {}", fila.get("id"), e);
            }
        }
        if (enlazados > 0 || !ediciones.isEmpty()) {
            log.info("Auditoría al día: {} registros enlazados, {} ediciones convertidas.", enlazados, ediciones.size());
        }
    }

    static List<Cambio> leerCambios(String detalle) {
        List<Cambio> cambios = new ArrayList<>();
        if (detalle == null || detalle.isBlank() || detalle.equals("Sin cambios")) {
            return cambios;
        }
        for (String parte : detalle.split("; ")) {
            int dosPuntos = parte.indexOf(": ");
            int flecha = parte.indexOf(" → ", Math.max(0, dosPuntos));
            if (dosPuntos > 0 && flecha > dosPuntos) {
                cambios.add(new Cambio(parte.substring(0, dosPuntos),
                        vacio(parte.substring(dosPuntos + 2, flecha)), vacio(parte.substring(flecha + 3))));
            } else if (parte.equals("Notas modificadas")) {
                cambios.add(new Cambio("Notas", null, null));
            } else if (parte.equals("Fotografía agregada")) {
                cambios.add(new Cambio("Fotografía", "Sin fotografía", "Fotografía nueva"));
            } else if (parte.equals("Fotografía quitada")) {
                cambios.add(new Cambio("Fotografía", "Con fotografía", "Sin fotografía"));
            } else if (parte.equals("Fotografía cambiada")) {
                cambios.add(new Cambio("Fotografía", "Fotografía anterior", "Fotografía nueva"));
            } else {
                cambios.add(new Cambio("Cambio", null, parte));
            }
        }
        return cambios;
    }

    private static String vacio(String valor) {
        return "(vacío)".equals(valor) ? null : valor;
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
