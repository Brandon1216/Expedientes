package com.expedientes.controller;

import com.expedientes.config.ApiException;
import com.expedientes.dto.Dtos.CambiarPasswordRequest;
import com.expedientes.dto.Dtos.LoginRequest;
import com.expedientes.dto.Dtos.LoginResponse;
import com.expedientes.dto.Dtos.UsuarioDto;
import com.expedientes.model.Usuario;
import com.expedientes.repository.AuditoriaRepository;
import com.expedientes.repository.UsuarioRepository;
import com.expedientes.security.AuthInterceptor;
import com.expedientes.security.LimiteIntentos;
import com.expedientes.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarios;
    private final TokenService tokens;
    private final BCryptPasswordEncoder encoder;
    private final LimiteIntentos limite;
    private final AuditoriaRepository auditoria;
    private final String passwordInicial;

    public AuthController(UsuarioRepository usuarios, TokenService tokens, BCryptPasswordEncoder encoder,
                          LimiteIntentos limite, AuditoriaRepository auditoria,
                          @Value("${app.admin.password}") String passwordInicial) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.encoder = encoder;
        this.limite = limite;
        this.auditoria = auditoria;
        this.passwordInicial = passwordInicial;
    }

    @GetMapping("/identidad")
    public Map<String, String> identidad() {
        return Map.of("programa", "expedientes");
    }

    @Transactional(noRollbackFor = ApiException.class)
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req, HttpServletRequest peticion) {
        String nombreUsuario = req.usuario() == null ? "" : req.usuario().trim();
        String password = req.password() == null ? "" : req.password();
        String equipo = peticion.getRemoteAddr();

        limite.revisar(equipo, nombreUsuario);
        Optional<Usuario> cuenta = usuarios.buscarPorUsuario(nombreUsuario);
        Usuario u = cuenta.filter(x -> encoder.matches(password, x.passwordHash())).orElse(null);
        if (u == null) {
            limite.registrarFallo(equipo, nombreUsuario);
            String escrito = nombreUsuario.length() > 60 ? nombreUsuario.substring(0, 60) : nombreUsuario;
            auditoria.registrar(cuenta.map(Usuario::id).orElse(null), escrito.isEmpty() ? "(vacío)" : escrito,
                    cuenta.map(Usuario::nombre).orElse(null), equipo, "INICIO_FALLIDO", null,
                    cuenta.isPresent() ? "Contraseña incorrecta" : "El usuario no existe");
            throw ApiException.noAutorizado("Usuario o contraseña incorrectos.");
        }
        limite.registrarExito(equipo, nombreUsuario);

        if (!u.activo()) {
            auditoria.registrar(u, equipo, "INICIO_FALLIDO", null, "Cuenta desactivada");
            throw ApiException.prohibido("Tu cuenta está desactivada. Contacta al administrador.");
        }
        long sesion = auditoria.abrirSesion(u, equipo);
        auditoria.registrar(u, equipo, "INICIO_SESION", null, null);
        CompletableFuture.runAsync(() -> {
            String nombre = nombreEquipo(equipo);
            if (nombre != null) {
                auditoria.nombrarEquipo(sesion, nombre);
            }
        });
        return new LoginResponse(tokens.generar(u.id(), sesion), UsuarioDto.de(u));
    }

    private static String nombreEquipo(String ip) {
        try {
            InetAddress direccion = InetAddress.getByName(ip);
            if (direccion.isLoopbackAddress()) {
                return null;
            }
            String nombre = direccion.getCanonicalHostName();
            if (nombre == null || nombre.equals(ip) || nombre.equals(direccion.getHostAddress())) {
                return null;
            }
            int punto = nombre.indexOf('.');
            return (punto > 0 ? nombre.substring(0, punto) : nombre).toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    @PostMapping("/salir")
    public void salir(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                      @RequestAttribute(AuthInterceptor.ATRIBUTO_SESION) Long sesion,
                      HttpServletRequest peticion) {
        if (auditoria.cerrarSesion(sesion, "Cerró sesión")) {
            auditoria.registrar(yo, peticion.getRemoteAddr(), "CIERRE_SESION", null, null);
        }
    }

    @GetMapping("/yo")
    public UsuarioDto yo(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo) {
        return UsuarioDto.de(yo);
    }

    @Transactional
    @PutMapping("/password")
    public void cambiarPassword(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                                @RequestBody CambiarPasswordRequest req, HttpServletRequest peticion) {
        if (req.actual() == null || !encoder.matches(req.actual(), yo.passwordHash())) {
            throw ApiException.invalido("La contraseña actual no es correcta.");
        }
        Validaciones.password(req.nueva());
        if (req.nueva().equals(req.actual())) {
            throw ApiException.invalido("La contraseña nueva debe ser distinta de la actual.");
        }
        if (req.nueva().equals(passwordInicial) || req.nueva().equalsIgnoreCase(yo.usuario())) {
            throw ApiException.invalido("Esa contraseña es muy fácil de adivinar. Elige otra.");
        }
        usuarios.cambiarPassword(yo.id(), encoder.encode(req.nueva()));
        auditoria.registrar(yo, peticion.getRemoteAddr(), "CAMBIO_PASSWORD", null, null);
    }
}
