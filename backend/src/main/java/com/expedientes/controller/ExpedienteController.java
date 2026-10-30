package com.expedientes.controller;

import com.expedientes.config.ApiException;
import com.expedientes.dto.Dtos.ExpedienteRequest;
import com.expedientes.dto.Dtos.Resumen;
import com.expedientes.model.Expediente;
import com.expedientes.model.Usuario;
import com.expedientes.repository.ExpedienteRepository;
import com.expedientes.security.AuthInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Todos los usuarios pueden consultar, crear y editar expedientes.
 * Solo los administradores pueden eliminarlos.
 */
@RestController
@RequestMapping("/api/expedientes")
public class ExpedienteController {

    private final ExpedienteRepository expedientes;

    public ExpedienteController(ExpedienteRepository expedientes) {
        this.expedientes = expedientes;
    }

    @GetMapping
    public List<Expediente> listar(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) String status) {
        return expedientes.buscar(q, status);
    }

    @GetMapping("/resumen")
    public Resumen resumen() {
        return expedientes.resumen();
    }

    @GetMapping("/{id}")
    public Expediente obtener(@PathVariable long id) {
        return expedientes.buscarPorId(id)
                .orElseThrow(() -> ApiException.noEncontrado("El expediente no existe."));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Expediente crear(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                            @RequestBody ExpedienteRequest req) {
        long id = expedientes.crear(validar(req, 0), yo.id());
        return obtener(id);
    }

    @PutMapping("/{id}")
    public Expediente actualizar(@PathVariable long id, @RequestBody ExpedienteRequest req) {
        obtener(id); // valida que exista
        expedientes.actualizar(id, validar(req, id));
        return obtener(id);
    }

    /** Devuelve los datos limpios o lanza error. id = 0 para un expediente nuevo. */
    private ExpedienteRequest validar(ExpedienteRequest req, long id) {
        String numero = Validaciones.requerido(req.numero(), "Número", 30);
        if (expedientes.numeroOcupado(numero, id)) {
            throw ApiException.invalido("Ya existe un expediente con el número " + numero + ".");
        }
        return new ExpedienteRequest(
                numero,
                Validaciones.status(req.status()),
                Validaciones.fecha(req.fechaIngreso()),
                Validaciones.requerido(req.nombre(), "Nombre y apellido", 150),
                Validaciones.fechaOpcional(req.fechaNacimiento()),
                Validaciones.opcional(req.solicitud(), 300),
                Validaciones.secretaria(req.secretaria()),
                Validaciones.opcional(req.perfil(), 150),
                Validaciones.opcional(req.telefono(), 30),
                Validaciones.opcional(req.familiarSindicalizado(), 150),
                Validaciones.opcional(req.parentesco(), 60),
                Validaciones.opcional(req.dependenciaFamiliar(), 150),
                Validaciones.opcional(req.notas(), 5000),
                Validaciones.fotografia(req.fotografia()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                         @PathVariable long id) {
        if (!yo.esAdmin()) {
            throw ApiException.prohibido("Solo un administrador puede eliminar expedientes.");
        }
        obtener(id);
        expedientes.eliminar(id);
    }
}
