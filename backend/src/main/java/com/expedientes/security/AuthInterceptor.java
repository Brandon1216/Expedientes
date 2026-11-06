package com.expedientes.security;

import com.expedientes.config.ApiException;
import com.expedientes.model.Usuario;
import com.expedientes.repository.AuditoriaRepository;
import com.expedientes.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_USUARIO = "usuarioActual";
    public static final String ATRIBUTO_SESION = "sesionActual";

    private static final Set<String> CON_PASSWORD_INICIAL = Set.of("/api/auth/yo", "/api/auth/password", "/api/auth/salir");

    private final TokenService tokenService;
    private final UsuarioRepository usuarios;
    private final AuditoriaRepository auditoria;

    public AuthInterceptor(TokenService tokenService, UsuarioRepository usuarios, AuditoriaRepository auditoria) {
        this.tokenService = tokenService;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String cabecera = request.getHeader("Authorization");
        String token = (cabecera != null && cabecera.startsWith("Bearer ")) ? cabecera.substring(7) : null;
        TokenService.Sesion sesion = tokenService.validar(token);
        if (sesion == null || !auditoria.sesionAbierta(sesion.idSesion(), sesion.idUsuario())) {
            throw ApiException.noAutorizado("Tu sesión expiró. Vuelve a iniciar sesión.");
        }

        Usuario usuario = usuarios.buscarPorId(sesion.idUsuario())
                .filter(Usuario::activo)
                .orElseThrow(() -> ApiException.noAutorizado("Tu cuenta no está activa."));

        String ruta = request.getRequestURI();
        if (usuario.debeCambiarPassword() && !CON_PASSWORD_INICIAL.contains(ruta)) {
            throw ApiException.prohibido("Debes cambiar tu contraseña antes de continuar.");
        }

        if (ruta.startsWith("/api/usuarios") && !usuario.esAdmin()) {
            throw ApiException.prohibido("Solo un administrador puede hacer esto.");
        }

        auditoria.registrarActividad(sesion.idSesion());
        request.setAttribute(ATRIBUTO_USUARIO, usuario);
        request.setAttribute(ATRIBUTO_SESION, sesion.idSesion());
        return true;
    }
}
