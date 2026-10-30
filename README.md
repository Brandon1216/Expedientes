# Sistema de Expedientes

React (frontend) + Java Spring Boot (backend) + SQLite (base de datos).

## Requisitos

- **Java 17 o superior** (`java -version`)
- **Maven** (`mvn -v`) — o abre la carpeta `backend` con la extensión "Extension Pack for Java" de VS Code
- **Node.js 18 o superior** (`node -v`)

## Cómo arrancarlo

Abre la carpeta `expedientes` en VS Code y usa **dos terminales**.

**Terminal 1 — backend:**

```bash
cd backend
mvn spring-boot:run
```

La primera vez descarga dependencias (tarda un par de minutos). Queda escuchando en `http://localhost:8080` y crea el archivo `expedientes.db` dentro de `backend/`.

**Terminal 2 — frontend:**

```bash
cd frontend
npm install
npm run dev
```

Abre `http://localhost:5173` en el navegador.

## Primer acceso

- Usuario: `admin`
- Contraseña: `admin123`

Cámbiala en cuanto entres (menú lateral → *Cambiar contraseña*). Desde **Usuarios** creas las cuentas de las demás personas.

## Permisos

| Acción | Usuario | Administrador |
|---|---|---|
| Ver y buscar expedientes | ✓ | ✓ |
| Crear y editar expedientes | ✓ | ✓ |
| Eliminar expedientes | | ✓ |
| Crear, editar, desactivar y eliminar usuarios | | ✓ |

El sistema no permite quedarse sin al menos un administrador activo.

## Estructura

```
expedientes/
├── backend/                      Java + Spring Boot
│   ├── pom.xml
│   └── src/main/
│       ├── resources/
│       │   ├── application.properties   configuración (puerto, clave, admin inicial)
│       │   └── schema.sql               tablas de SQLite
│       └── java/com/expedientes/
│           ├── controller/   endpoints de la API
│           ├── repository/   consultas SQL
│           ├── security/     tokens de sesión y control de acceso
│           ├── model/  dto/  config/
└── frontend/                     React + Vite
    └── src/
        ├── pages/        Login, Expedientes, Usuarios, CambiarPassword
        ├── components/   Marco (barra lateral), Carpeta (detalle), Diálogos, Avisos
        ├── api.js        llamadas al backend
        └── styles.css    todo el diseño
```

## API

Todas las rutas (excepto el login) requieren el encabezado `Authorization: Bearer <token>`.

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | `{usuario, password}` → `{token, usuario}` |
| GET | `/api/auth/yo` | Usuario de la sesión |
| PUT | `/api/auth/password` | `{actual, nueva}` |
| GET | `/api/expedientes?q=&status=` | Lista con búsqueda y filtro (`ACTIVO` / `INACTIVO`) |
| GET | `/api/expedientes/resumen` | Conteo por status |
| GET/PUT/DELETE | `/api/expedientes/{id}` | Ver, editar, eliminar (solo admin) |
| POST | `/api/expedientes` | Crear (el número lo captura el usuario y no se puede repetir) |
| GET/POST | `/api/usuarios` | Solo admin |
| PUT/DELETE | `/api/usuarios/{id}` | Solo admin |

## Antes de usarlo en serio

1. En `application.properties` cambia `app.token.secret` por una frase larga y aleatoria.
2. Respalda `backend/expedientes.db`: ese archivo **es** toda tu base de datos. Para verla puedes usar la extensión "SQLite Viewer" de VS Code o DB Browser for SQLite.

## Ideas para crecer

- Adjuntar archivos (PDF, imágenes) a cada expediente
- Historial de cambios por expediente
- Exportar la lista a Excel
- Campos extra (CURP, teléfono, área responsable…): agrégalos en `schema.sql`, `ExpedienteRepository`, `Dtos.ExpedienteRequest` y `Carpeta.jsx`
