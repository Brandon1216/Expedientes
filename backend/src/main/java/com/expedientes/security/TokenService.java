package com.expedientes.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Tokens de sesión firmados con HMAC-SHA256 (mismo principio que un JWT).
 * Formato: base64url(idUsuario|expiracion) + "." + base64url(firma)
 */
@Component
public class TokenService {

    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private final byte[] secreto;
    private final long segundosValidez;

    public TokenService(@Value("${app.token.secret}") String secreto,
                        @Value("${app.token.horas}") long horas) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
        this.segundosValidez = horas * 3600;
    }

    public String generar(long idUsuario) {
        long expira = Instant.now().getEpochSecond() + segundosValidez;
        String datos = ENC.encodeToString((idUsuario + "|" + expira).getBytes(StandardCharsets.UTF_8));
        return datos + "." + firmar(datos);
    }

    /** Devuelve el id del usuario si el token es válido y no ha expirado; si no, null. */
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
