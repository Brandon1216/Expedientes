package com.expedientes.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
public class Respaldos {

    private static final Logger log = LoggerFactory.getLogger(Respaldos.class);

    private final JdbcTemplate jdbc;
    private final Path carpeta;
    private final int conservar;

    public Respaldos(JdbcTemplate jdbc,
                     @Value("${app.respaldos.carpeta}") String carpeta,
                     @Value("${app.respaldos.conservar}") int conservar) {
        this.jdbc = jdbc;
        this.carpeta = Path.of(carpeta);
        this.conservar = conservar;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 3 * 60 * 60_000)
    public void respaldarSiFalta() {
        try {
            Files.createDirectories(carpeta);
            Path hoy = carpeta.resolve("expedientes-" + LocalDate.now() + ".db");
            if (Files.exists(hoy)) {
                return;
            }
            copiarBase(hoy);
            log.info("Respaldo creado: {}", hoy);
            borrarAntiguos();
        } catch (Exception e) {
            log.error("No se pudo crear el respaldo en {}", carpeta, e);
        }
    }

    void copiarBase(Path destino) {
        String ruta = destino.toAbsolutePath().toString().replace('\\', '/').replace("'", "''");
        jdbc.execute("VACUUM INTO '" + ruta + "'");
    }

    private void borrarAntiguos() throws IOException {
        List<Path> respaldos;
        try (Stream<Path> archivos = Files.list(carpeta)) {
            respaldos = archivos
                    .filter(p -> p.getFileName().toString().matches("expedientes-\\d{4}-\\d{2}-\\d{2}\\.db"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .toList();
        }
        for (Path viejo : respaldos.subList(Math.min(conservar, respaldos.size()), respaldos.size())) {
            Files.deleteIfExists(viejo);
        }
    }
}
