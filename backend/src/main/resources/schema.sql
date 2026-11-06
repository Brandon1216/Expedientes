CREATE TABLE IF NOT EXISTS usuarios (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario       TEXT    NOT NULL UNIQUE,
    nombre        TEXT    NOT NULL,
    password_hash TEXT    NOT NULL,
    rol           TEXT    NOT NULL CHECK (rol IN ('ADMIN', 'USUARIO')),
    activo        INTEGER NOT NULL DEFAULT 1,
    creado_en     TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    debe_cambiar_password INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS copias_usb (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    fecha    TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    unidad   TEXT    NOT NULL,
    archivo  TEXT    NOT NULL,
    bytes    INTEGER NOT NULL,
    usuario  TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS sesiones (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id       INTEGER NOT NULL,
    usuario          TEXT    NOT NULL,
    nombre           TEXT,
    equipo           TEXT,
    equipo_nombre    TEXT,
    inicio           TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    ultima_actividad TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    fin              TEXT,
    motivo_fin       TEXT
);

CREATE TABLE IF NOT EXISTS auditoria (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    fecha      TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    usuario_id INTEGER,
    usuario    TEXT    NOT NULL,
    nombre     TEXT,
    equipo     TEXT,
    accion     TEXT    NOT NULL,
    expediente_id INTEGER,
    objeto     TEXT,
    detalle    TEXT,
    cambios    TEXT
);

CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria(fecha);
CREATE INDEX IF NOT EXISTS idx_sesiones_inicio ON sesiones(inicio);

CREATE TABLE IF NOT EXISTS expedientes (
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
);
