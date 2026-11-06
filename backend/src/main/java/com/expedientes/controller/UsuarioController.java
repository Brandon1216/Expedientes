package com.expedientes.controller;

import com.expedientes.config.ApiException;
import com.expedientes.config.Escritorio;
import com.expedientes.dto.Dtos.UsuarioDto;
import com.expedientes.dto.Dtos.UsuarioRequest;
import com.expedientes.model.Usuario;
import com.expedientes.repository.AuditoriaRepository;
import com.expedientes.repository.UsuarioRepository;
import com.expedientes.security.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private static final Object CUENTAS = new Object();

    private final UsuarioRepository usuarios;
    private final BCryptPasswordEncoder encoder;
    private final AuditoriaRepository auditoria;

    private final int puerto;

    public UsuarioController(UsuarioRepository usuarios, BCryptPasswordEncoder encoder, AuditoriaRepository auditoria,
                             @Value("${server.port}") int puerto) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.auditoria = auditoria;
        this.puerto = puerto;
    }

    @GetMapping("/direccion-red")
    public Map<String, String> direccionRed() {
        return Map.of("direccion", Escritorio.direccionRed(puerto));
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return usuarios.listar().stream().map(UsuarioDto::de).toList();
    }

    @Transactional
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto crear(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                            @RequestBody UsuarioRequest req, HttpServletRequest peticion) {
        String nombreUsuario = Validaciones.nombreUsuario(req.usuario());
        if (usuarios.buscarPorUsuario(nombreUsuario).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre.");
        }
        Validaciones.password(req.password());
        String rol = Validaciones.rol(req.rol());
        usuarios.crear(nombreUsuario,
                Validaciones.requerido(req.nombre(), "Nombre", 120),
                encoder.encode(req.password()),
                rol);
        Usuario nuevo = usuarios.buscarPorUsuario(nombreUsuario).orElseThrow();
        auditoria.registrar(yo, peticion.getRemoteAddr(), "CREAR_USUARIO", etiqueta(nuevo), "Rol: " + rolVisible(rol));
        return UsuarioDto.de(nuevo);
    }

    @Transactional
    @PutMapping("/{id}")
    public UsuarioDto actualizar(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                                 @PathVariable long id, @RequestBody UsuarioRequest req,
                                 HttpServletRequest peticion) {
        synchronized (CUENTAS) {
            return actualizarEnOrden(yo, id, req, peticion);
        }
    }

    private UsuarioDto actualizarEnOrden(Usuario yo, long id, UsuarioRequest req, HttpServletRequest peticion) {
        if (!usuarios.buscarPorId(yo.id()).map(Usuario::activo).orElse(false)) {
            throw ApiException.noAutorizado("Tu cuenta no está activa.");
        }
        Usuario actual = buscar(id);
        String rol = Validaciones.rol(req.rol());
        boolean activo = req.activo() == null || req.activo();

        boolean dejaDeSerAdminActivo = actual.esAdmin() && actual.activo() && (!"ADMIN".equals(rol) || !activo);
        if (dejaDeSerAdminActivo && usuarios.contarAdminsActivos() <= 1) {
            throw ApiException.invalido("Debe quedar al menos un administrador activo.");
        }
        if (actual.id().equals(yo.id()) && !activo) {
            throw ApiException.invalido("No puedes desactivar tu propia cuenta.");
        }

        String nombre = Validaciones.requerido(req.nombre(), "Nombre", 120);
        boolean restablece = req.password() != null && !req.password().isEmpty();
        if (restablece) {
            Validaciones.password(req.password());
        }
        usuarios.actualizar(id, nombre, rol, activo);
        if (restablece) {
            usuarios.cambiarPassword(id, encoder.encode(req.password()));
        }

        List<String> cambios = new ArrayList<>();
        if (!actual.nombre().equals(nombre)) {
            cambios.add("Nombre: " + actual.nombre() + " → " + nombre);
        }
        if (!actual.rol().equals(rol)) {
            cambios.add("Rol: " + rolVisible(actual.rol()) + " → " + rolVisible(rol));
        }
        if (actual.activo() != activo) {
            cambios.add(activo ? "Cuenta activada" : "Cuenta desactivada");
        }
        if (restablece) {
            cambios.add("Contraseña restablecida");
        }
        auditoria.registrar(yo, peticion.getRemoteAddr(), "EDITAR_USUARIO", etiqueta(actual),
                cambios.isEmpty() ? "Sin cambios" : String.join("; ", cambios));

        if (!yo.id().equals(id)) {
            if (actual.activo() && !activo) {
                auditoria.cerrarSesionesDe(id, "Cuenta desactivada");
            } else if (restablece) {
                auditoria.cerrarSesionesDe(id, "Contraseña restablecida");
            }
        }
        return UsuarioDto.de(buscar(id));
    }

    @Transactional
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo, @PathVariable long id,
                         HttpServletRequest peticion) {
        synchronized (CUENTAS) {
            eliminarEnOrden(yo, id, peticion);
        }
    }

    private void eliminarEnOrden(Usuario yo, long id, HttpServletRequest peticion) {
        if (!usuarios.buscarPorId(yo.id()).map(Usuario::activo).orElse(false)) {
            throw ApiException.noAutorizado("Tu cuenta no está activa.");
        }
        Usuario u = buscar(id);
        if (u.id().equals(yo.id())) {
            throw ApiException.invalido("No puedes eliminar tu propia cuenta.");
        }
        if (u.esAdmin() && u.activo() && usuarios.contarAdminsActivos() <= 1) {
            throw ApiException.invalido("Debe quedar al menos un administrador activo.");
        }
        usuarios.eliminar(id);
        auditoria.cerrarSesionesDe(id, "Cuenta eliminada");
        auditoria.registrar(yo, peticion.getRemoteAddr(), "ELIMINAR_USUARIO", etiqueta(u), null);
    }

    private static String etiqueta(Usuario u) {
        return u.usuario() + " · " + u.nombre();
    }

    private static String rolVisible(String rol) {
        return "ADMIN".equals(rol) ? "Administrador" : "Usuario";
    }

    private Usuario buscar(long id) {
        return usuarios.buscarPorId(id).orElseThrow(() -> ApiException.noEncontrado("El usuario no existe."));
    }
}
