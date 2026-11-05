package com.expedientes.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Component
public class TokenService {

    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private final byte[] secreto;
    private final long segundosValidez;

    public TokenService(@Value("${app.datos}") String carpetaDatos,
                        @Value("${app.token.horas}") long horas) {
        this.secreto = cargarClave(Path.of(carpetaDatos, "clave-sesiones.key"));
        this.segundosValidez = horas * 3600;
    }

    private static byte[] cargarClave(Path archivo) {
        try {
            Files.createDirectories(archivo.getParent());
            if (Files.exists(archivo) && Files.size(archivo) >= 32) {
                return Files.readAllBytes(archivo);
            }
            byte[] aleatorios = new byte[32];
            new SecureRandom().nextBytes(aleatorios);
            byte[] clave = ENC.encode(aleatorios);
            Files.write(archivo, clave);
            return clave;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo preparar la clave de sesiones en " + archivo, e);
        }
    }

    public String generar(long idUsuario) {
        long expira = Instant.now().getEpochSecond() + segundosValidez;
        String datos = ENC.encodeToString((idUsuario + "|" + expira).getBytes(StandardCharsets.UTF_8));
        return datos + "." + firmar(datos);
    }

    public Long validar(String token) {
        if (token == null) {
            return null;
        }
        String[] partes = token.split("\\.");
        if (partes.length != 2) {
            return null;
        }
        byte[] esperada = firmar(partes[0]).getBytes(StandardCharsets.UTF_8);
        byte[] recibida = partes[1].getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(esperada, recibida)) {
            return null;
        }
        try {
            String[] datos = new String(DEC.decode(partes[0]), StandardCharsets.UTF_8).split("\\|");
            long id = Long.parseLong(datos[0]);
            long expira = Long.parseLong(datos[1]);
            return Instant.now().getEpochSecond() < expira ? id : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String firmar(String datos) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return ENC.encodeToString(mac.doFinal(datos.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }
}
