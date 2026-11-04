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

-- Registro de los respaldos copiados a USB
CREATE TABLE IF NOT EXISTS copias_usb (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    fecha    TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    unidad   TEXT    NOT NULL,
    archivo  TEXT    NOT NULL,
    bytes    INTEGER NOT NULL,
    usuario  TEXT    NOT NULL
);

-- Las bases con el formato anterior (titular, asunto...) las convierte DatosIniciales al arrancar.
-- El índice de status también se crea ahí, después de esa conversión.
CREATE TABLE IF NOT EXISTS expedientes (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    numero                  TEXT    UNIQUE,
    status                  TEXT    NOT NULL DEFAULT 'ACTIVO' CHECK (status IN ('ACTIVO', 'INACTIVO')),
    fecha_ingreso           TEXT    NOT NULL,
    nombre                  TEXT    NOT NULL,   -- nombre y apellido
    fecha_nacimiento        TEXT,
    solicitud               TEXT,
    secretaria              TEXT,               -- secretaría asignada
    perfil                  TEXT,
    telefono                TEXT,
    familiar_sindicalizado  TEXT,               -- nombre del familiar
    parentesco              TEXT,
    dependencia_familiar    TEXT,
    notas                   TEXT,
    fotografia              TEXT,               -- imagen JPEG como data URL (data:image/jpeg;base64,...)
    creado_por              INTEGER REFERENCES usuarios(id),
    creado_en               TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    actualizado_en          TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);
