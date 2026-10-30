package com.expedientes.security;

import com.expedientes.config.ApiException;
import com.expedientes.model.Usuario;
import com.expedientes.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Revisa el token en cada petición a /api/** (excepto el login).
 * Las rutas /api/usuarios/** quedan reservadas para administradores.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_USUARIO = "usuarioActual";

    private final TokenService tokenService;
    private final UsuarioRepository usuarios;

    public AuthInterceptor(TokenService tokenService, UsuarioRepository usuarios) {
        this.tokenService = tokenService;
        this.usuarios = usuarios;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true; // preflight de CORS
        }

        String cabecera = request.getHeader("Authorization");
        String token = (cabecera != null && cabecera.startsWith("Bearer ")) ? cabecera.substring(7) : null;
        Long id = tokenService.validar(token);
        if (id == null) {
            throw ApiException.noAutorizado("Tu sesión expiró. Vuelve a iniciar sesión.");
        }

        Usuario usuario = usuarios.buscarPorId(id)
                .filter(Usuario::activo)
                .orElseThrow(() -> ApiException.noAutorizado("Tu cuenta no está activa."));

        if (request.getRequestURI().startsWith("/api/usuarios") && !usuario.esAdmin()) {
            throw ApiException.prohibido("Solo un administrador puede hacer esto.");
        }

        request.setAttribute(ATRIBUTO_USUARIO, usuario);
        return true;
    }
}
