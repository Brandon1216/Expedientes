package com.expedientes.config;

import com.expedientes.dto.Dtos.CopiaHecha;
import com.expedientes.dto.Dtos.UnidadUsb;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Copia un respaldo a una USB conectada a la computadora donde corre el programa.
 * Solo escribe en unidades detectadas como USB: nunca en una ruta recibida del navegador.
 */
@Component
public class CopiaUsb {

    private static final Logger log = LoggerFactory.getLogger(CopiaUsb.class);
    private static final String CARPETA_EN_USB = "Respaldos Expedientes";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    // Unidades extraíbles, más discos cuyo bus es USB (Windows registra algunas USB como disco fijo)
    private static final String DETECTAR = """
            [Console]::OutputEncoding=[Text.Encoding]::UTF8;
            $letras=@();
            try{$letras+=Get-Disk -ErrorAction Stop|Where-Object BusType -eq 'USB'|Get-Partition|Where-Object DriveLetter|ForEach-Object{$_.DriveLetter.ToString()+':'}}catch{};
            $unidades=@(Get-CimInstance Win32_LogicalDisk|Where-Object{$_.DriveType -eq 2 -or $letras -contains $_.DeviceID}|Select-Object DeviceID,VolumeName,FileSystem,FreeSpace);
            ConvertTo-Json -InputObject $unidades -Compress""";

    private final Respaldos respaldos;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final Path carpetaRespaldos;
    private final Path carpetaDatos;
    private final String usbPrueba;
    private final AtomicBoolean copiando = new AtomicBoolean();

    public CopiaUsb(Respaldos respaldos, JdbcTemplate jdbc, ObjectMapper json,
                    @Value("${app.respaldos.carpeta}") String carpetaRespaldos,
                    @Value("${app.datos}") String carpetaDatos,
                    // Solo para pruebas automáticas: carpetas que se tratan como si fueran USB
                    @Value("${app.respaldos.usb-prueba:}") String usbPrueba) {
        this.respaldos = respaldos;
        this.jdbc = jdbc;
        this.json = json;
        this.carpetaRespaldos = Path.of(carpetaRespaldos);
        this.carpetaDatos = Path.of(carpetaDatos);
        this.usbPrueba = usbPrueba;
    }

    public List<UnidadUsb> unidades() {
        return usbPrueba.isBlank() ? detectar() : unidadesDePrueba();
    }

    private List<UnidadUsb> unidadesDePrueba() {
        return Arrays.stream(usbPrueba.split(",")).map(ruta -> {
            Path p = Path.of(ruta);
            return Files.isDirectory(p)
                    ? new UnidadUsb(ruta, "USB de prueba", true, libre(p))
                    : new UnidadUsb(ruta, "USB de prueba", false, 0);
        }).toList();
    }

    private static long libre(Path p) {
        try {
            return Files.getFileStore(p).getUsableSpace();
        } catch (IOException e) {
            return 0;
        }
    }

    private List<UnidadUsb> detectar() {
        Process proceso = null;
        try {
            proceso = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", DETECTAR)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            String salida;
            try (InputStream entrada = proceso.getInputStream()) {
                salida = new String(entrada.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
            if (!proceso.waitFor(15, TimeUnit.SECONDS) || salida.isEmpty()) {
                return List.of();
            }
            String unidadDatos = carpetaDatos.toAbsolutePath().getRoot().toString().replace("\\", "");
            List<UnidadUsb> unidades = new ArrayList<>();
            for (JsonNode n : json.readTree(salida)) {
                String id = n.path("DeviceID").asText();
                if (id.equalsIgnoreCase(unidadDatos)) {
                    continue; // copiar al mismo disco no protege nada
                }
                // Sin sistema de archivos: cifrada y sin desbloquear, o lector sin tarjeta
                boolean legible = !n.path("FileSystem").asText("").isBlank();
                unidades.add(new UnidadUsb(id, n.path("VolumeName").asText(""), legible, n.path("FreeSpace").asLong(0)));
            }
            return unidades;
        } catch (IOException e) {
            log.warn("No se pudieron detectar las USB", e);
            return List.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } finally {
            if (proceso != null) {
                proceso.destroyForcibly();
            }
        }
    }

    /** Hace una copia de la base en este momento, la pasa a la USB y comprueba que quedó idéntica. */
    public CopiaHecha copiar(String idUnidad, String usuario) {
        UnidadUsb unidad = unidades().stream().filter(u -> u.id().equals(idUnidad)).findFirst()
                .orElseThrow(() -> ApiException.invalido("Esa USB ya no está conectada a la computadora principal."));
        if (!unidad.disponible()) {
            throw ApiException.invalido("No se puede leer la USB. Si está cifrada, desbloquéala e inténtalo de nuevo.");
        }
        if (!copiando.compareAndSet(false, true)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya se está copiando un respaldo. Espera a que termine.");
        }

        String nombre = "expedientes-" + LocalDateTime.now().format(FORMATO) + ".db";
        Path temporal = carpetaRespaldos.resolve(nombre + ".tmp");
        Path carpeta = rutaUnidad(idUnidad).resolve(CARPETA_EN_USB);
        Path parcial = carpeta.resolve(nombre + ".parcial");
        try {
            Files.createDirectories(carpetaRespaldos);
            respaldos.copiarBase(temporal);
            long tamano = Files.size(temporal);

            long libre = libre(rutaUnidad(idUnidad));
            if (libre < tamano) {
                throw ApiException.invalido("La USB no tiene espacio suficiente: faltan "
                        + megas(tamano - libre) + ". No se copió nada.");
            }

            Files.createDirectories(carpeta);
            Files.copy(temporal, parcial, StandardCopyOption.REPLACE_EXISTING);
            try (FileChannel canal = FileChannel.open(parcial, StandardOpenOption.WRITE)) {
                canal.force(true); // que llegue a la USB antes de comprobarla
            }
            if (!huella(temporal).equals(huella(parcial))) {
                throw ApiException.invalido("La copia en la USB no quedó igual al original. Prueba con otra USB.");
            }
            Files.move(parcial, carpeta.resolve(nombre), StandardCopyOption.ATOMIC_MOVE);

            String archivo = CARPETA_EN_USB + "\\" + nombre;
            jdbc.update("INSERT INTO copias_usb (unidad, archivo, bytes, usuario) VALUES (?, ?, ?, ?)",
                    idUnidad, archivo, tamano, usuario);
            return historial().get(0);
        } catch (IOException e) {
            log.error("No se pudo copiar el respaldo a {}", idUnidad, e);
            throw ApiException.invalido("No se pudo escribir en la USB. Revisa que no esté protegida y que siga conectada.");
        } finally {
            borrar(temporal);
            borrar(parcial);
            copiando.set(false);
        }
    }

    private Path rutaUnidad(String id) {
        return usbPrueba.isBlank() ? Path.of(id + "\\") : Path.of(id);
    }

    private static void borrar(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            log.warn("No se pudo borrar {}", archivo, e);
        }
    }

    private static String megas(long bytes) {
        return Math.max(1, bytes / (1024 * 1024)) + " MB";
    }

    private static String huella(Path archivo) throws IOException {
        try (DigestInputStream entrada = new DigestInputStream(Files.newInputStream(archivo),
                MessageDigest.getInstance("SHA-256"))) {
            entrada.transferTo(java.io.OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(entrada.getMessageDigest().digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<CopiaHecha> historial() {
        return jdbc.query("SELECT fecha, unidad, archivo, bytes, usuario FROM copias_usb ORDER BY id DESC LIMIT 5",
                (rs, i) -> new CopiaHecha(rs.getString("fecha"), rs.getString("unidad"), rs.getString("archivo"),
                        rs.getLong("bytes"), rs.getString("usuario")));
    }

    /** Días desde la última copia, o null si nunca se ha hecho. */
    public Integer diasDesdeUltima() {
        return jdbc.queryForObject("""
                SELECT CAST(julianday('now', 'localtime') - julianday(MAX(fecha)) AS INTEGER) FROM copias_usb""",
                Integer.class);
    }
}
