package com.expedientes.security;

import com.expedientes.config.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LimiteIntentos {

    private static final int MAXIMO_POR_CUENTA = 5;
    private static final int MAXIMO_POR_EQUIPO = 20;
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private record Fallos(int cuenta, Instant ultimo) {
        boolean vencido() {
            return Instant.now().isAfter(ultimo.plus(BLOQUEO));
        }
    }

    private final Map<String, Fallos> fallos = new ConcurrentHashMap<>();

    public void revisar(String equipo, String usuario) {
        revisar(claveCuenta(equipo, usuario), MAXIMO_POR_CUENTA);
        revisar(equipo, MAXIMO_POR_EQUIPO);
    }

    private void revisar(String clave, int maximo) {
        Fallos f = fallos.get(clave);
        if (f != null && !f.vencido() && f.cuenta() >= maximo) {
            long minutos = Duration.between(Instant.now(), f.ultimo().plus(BLOQUEO)).toMinutes() + 1;
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Espera " + minutos + " min e inténtalo de nuevo.");
        }
    }

    public void registrarFallo(String equipo, String usuario) {
        fallos.values().removeIf(Fallos::vencido);
        sumar(claveCuenta(equipo, usuario));
        sumar(equipo);
    }

    private void sumar(String clave) {
        fallos.merge(clave, new Fallos(1, Instant.now()), (a, b) -> new Fallos(a.cuenta() + 1, b.ultimo()));
    }

    public void registrarExito(String equipo, String usuario) {
        fallos.remove(claveCuenta(equipo, usuario));
    }

    private static String claveCuenta(String equipo, String usuario) {
        return equipo + "|" + usuario.toLowerCase(Locale.ROOT);
    }
}
