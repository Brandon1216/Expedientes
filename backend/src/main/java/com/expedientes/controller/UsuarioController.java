package com.expedientes.controller;

import com.expedientes.config.ApiException;
import com.expedientes.dto.Dtos.UsuarioDto;
import com.expedientes.dto.Dtos.UsuarioRequest;
import com.expedientes.model.Usuario;
import com.expedientes.repository.UsuarioRepository;
import com.expedientes.security.AuthInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

import java.util.List;

/** Administración de usuarios. El AuthInterceptor ya garantiza que quien llama es ADMIN. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarios;
    private final BCryptPasswordEncoder encoder;

    public UsuarioController(UsuarioRepository usuarios, BCryptPasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return usuarios.listar().stream().map(UsuarioDto::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto crear(@RequestBody UsuarioRequest req) {
        String nombreUsuario = Validaciones.nombreUsuario(req.usuario());
        if (usuarios.buscarPorUsuario(nombreUsuario).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre.");
        }
        Validaciones.password(req.password());
        usuarios.crear(nombreUsuario,
                Validaciones.requerido(req.nombre(), "Nombre", 120),
                encoder.encode(req.password()),
                Validaciones.rol(req.rol()));
        return UsuarioDto.de(usuarios.buscarPorUsuario(nombreUsuario).orElseThrow());
    }

    @PutMapping("/{id}")
    public UsuarioDto actualizar(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                                 @PathVariable long id, @RequestBody UsuarioRequest req) {
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

        usuarios.actualizar(id, Validaciones.requerido(req.nombre(), "Nombre", 120), rol, activo);

        // Si el admin escribió una contraseña nueva, se restablece
        if (req.password() != null && !req.password().isEmpty()) {
            Validaciones.password(req.password());
            usuarios.cambiarPassword(id, encoder.encode(req.password()));
        }
        return UsuarioDto.de(buscar(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo, @PathVariable long id) {
        Usuario u = buscar(id);
        if (u.id().equals(yo.id())) {
            throw ApiException.invalido("No puedes eliminar tu propia cuenta.");
        }
        if (u.esAdmin() && u.activo() && usuarios.contarAdminsActivos() <= 1) {
            throw ApiException.invalido("Debe quedar al menos un administrador activo.");
        }
        usuarios.eliminar(id);
    }

    private Usuario buscar(long id) {
        return usuarios.buscarPorId(id).orElseThrow(() -> ApiException.noEncontrado("El usuario no existe."));
    }
}
