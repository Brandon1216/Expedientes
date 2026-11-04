package com.expedientes.controller;

import com.expedientes.config.ApiException;
import com.expedientes.dto.Dtos.CambiarPasswordRequest;
import com.expedientes.dto.Dtos.LoginRequest;
import com.expedientes.dto.Dtos.LoginResponse;
import com.expedientes.dto.Dtos.UsuarioDto;
import com.expedientes.model.Usuario;
import com.expedientes.repository.UsuarioRepository;
import com.expedientes.security.AuthInterceptor;
import com.expedientes.security.LimiteIntentos;
import com.expedientes.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarios;
    private final TokenService tokens;
    private final BCryptPasswordEncoder encoder;
    private final LimiteIntentos limite;
    private final String passwordInicial;

    public AuthController(UsuarioRepository usuarios, TokenService tokens, BCryptPasswordEncoder encoder,
                          LimiteIntentos limite, @Value("${app.admin.password}") String passwordInicial) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.encoder = encoder;
        this.limite = limite;
        this.passwordInicial = passwordInicial;
    }

    /** Público: permite saber que quien atiende el puerto es este programa y no otro. */
    @GetMapping("/identidad")
    public Map<String, String> identidad() {
        return Map.of("programa", "expedientes");
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req, HttpServletRequest peticion) {
        String nombreUsuario = req.usuario() == null ? "" : req.usuario().trim();
        String password = req.password() == null ? "" : req.password();
        String equipo = peticion.getRemoteAddr();

        limite.revisar(equipo, nombreUsuario);
        Usuario u = usuarios.buscarPorUsuario(nombreUsuario)
                .filter(x -> encoder.matches(password, x.passwordHash()))
                .orElse(null);
        if (u == null) {
            limite.registrarFallo(equipo, nombreUsuario);
            throw ApiException.noAutorizado("Usuario o contraseña incorrectos.");
        }
        limite.registrarExito(equipo, nombreUsuario);

        if (!u.activo()) {
            throw ApiException.prohibido("Tu cuenta está desactivada. Contacta al administrador.");
        }
        return new LoginResponse(tokens.generar(u.id()), UsuarioDto.de(u));
    }

    @GetMapping("/yo")
    public UsuarioDto yo(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo) {
        return UsuarioDto.de(yo);
    }

    @PutMapping("/password")
    public void cambiarPassword(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                                @RequestBody CambiarPasswordRequest req) {
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
    }
}
