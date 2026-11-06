package com.expedientes.controller;

import com.expedientes.config.ApiException;
import com.expedientes.dto.Dtos.Cambio;
import com.expedientes.dto.Dtos.ExpedienteRequest;
import com.expedientes.dto.Dtos.Resumen;
import com.expedientes.model.Expediente;
import com.expedientes.model.Usuario;
import com.expedientes.repository.AuditoriaRepository;
import com.expedientes.repository.ExpedienteRepository;
import com.expedientes.security.AuthInterceptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/expedientes")
public class ExpedienteController {

    private final ExpedienteRepository expedientes;
    private final AuditoriaRepository auditoria;
    private final ObjectMapper json;

    public ExpedienteController(ExpedienteRepository expedientes, AuditoriaRepository auditoria, ObjectMapper json) {
        this.expedientes = expedientes;
        this.auditoria = auditoria;
        this.json = json;
    }

    @GetMapping
    public List<Expediente> listar(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String secretaria) {
        return expedientes.buscar(q, status, secretaria);
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

    @Transactional
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Expediente crear(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                            @RequestBody ExpedienteRequest req, HttpServletRequest peticion) {
        long id = expedientes.crear(validar(req, 0), yo.id());
        Expediente nuevo = obtener(id);
        auditoria.registrarExpediente(yo, peticion.getRemoteAddr(), "CREAR_EXPEDIENTE", id, etiqueta(nuevo), null,
                null);
        return nuevo;
    }

    @Transactional
    @PutMapping("/{id}")
    public Expediente actualizar(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                                 @PathVariable long id, @RequestBody ExpedienteRequest req,
                                 HttpServletRequest peticion) {
        Expediente antes = obtener(id);
        ExpedienteRequest datos = validar(req, id);
        String versionVista = req.actualizadoEn() == null || req.actualizadoEn().isBlank() ? null : req.actualizadoEn();
        if (!expedientes.actualizar(id, datos, versionVista)) {
            if (expedientes.buscarPorId(id).isEmpty()) {
                throw ApiException.noEncontrado("El expediente no existe.");
            }
            throw new ApiException(HttpStatus.CONFLICT,
                    "Otra persona modificó este expediente mientras lo tenías abierto. Ciérralo y vuelve a abrirlo para ver los cambios.");
        }
        Expediente despues = obtener(id);
        List<Cambio> cambios = cambios(antes, datos);
        String resumen = cambios.isEmpty() ? "Sin cambios" : String.join("; ", cambios.stream().map(
                c -> c.antes() == null && c.despues() == null ? c.campo() + " modificadas"
                        : c.campo() + ": " + mostrar(c.antes(), 60) + " → " + mostrar(c.despues(), 60)).toList());
        String json;
        try {
            json = this.json.writeValueAsString(cambios);
        } catch (JsonProcessingException e) {
            json = null;
        }
        auditoria.registrarExpediente(yo, peticion.getRemoteAddr(), "EDITAR_EXPEDIENTE", id, etiqueta(despues),
                resumen, json);
        return despues;
    }

    private static String etiqueta(Expediente e) {
        return e.numero() + " · " + e.nombre();
    }

    private static List<Cambio> cambios(Expediente a, ExpedienteRequest d) {
        List<Cambio> c = new ArrayList<>();
        comparar(c, "Número", a.numero(), d.numero());
        comparar(c, "Status", a.status(), d.status());
        comparar(c, "Fecha de ingreso", a.fechaIngreso(), d.fechaIngreso());
        comparar(c, "Nombre", a.nombre(), d.nombre());
        comparar(c, "Fecha de nacimiento", a.fechaNacimiento(), d.fechaNacimiento());
        comparar(c, "Solicitud", a.solicitud(), d.solicitud());
        comparar(c, "Secretaría", a.secretaria(), d.secretaria());
        comparar(c, "Perfil", a.perfil(), d.perfil());
        comparar(c, "Teléfono", a.telefono(), d.telefono());
        comparar(c, "Familiar sindicalizado", a.familiarSindicalizado(), d.familiarSindicalizado());
        comparar(c, "Parentesco", a.parentesco(), d.parentesco());
        comparar(c, "Dependencia", a.dependenciaFamiliar(), d.dependenciaFamiliar());
        comparar(c, "Notas", a.notas(), d.notas());
        if (d.fotografia() != null) {
            if (d.fotografia().isEmpty()) {
                if (a.fotografia() != null) {
                    c.add(new Cambio("Fotografía", "Con fotografía", "Sin fotografía"));
                }
            } else {
                c.add(new Cambio("Fotografía", a.fotografia() == null ? "Sin fotografía" : "Fotografía anterior",
                        "Fotografía nueva"));
            }
        }
        return c;
    }

    private static void comparar(List<Cambio> cambios, String campo, String antes, String despues) {
        if (!Objects.equals(antes, despues)) {
            cambios.add(new Cambio(campo, recortar(antes, 300), recortar(despues, 300)));
        }
    }

    private static String recortar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        return valor.length() > maximo ? valor.substring(0, maximo) + "…" : valor;
    }

    private static String mostrar(String valor, int maximo) {
        return valor == null ? "(vacío)" : recortar(valor, maximo);
    }

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
                Validaciones.fotografia(req.fotografia()),
                null);
    }

    @Transactional
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                         @PathVariable long id, HttpServletRequest peticion) {
        if (!yo.esAdmin()) {
            throw ApiException.prohibido("Solo un administrador puede eliminar expedientes.");
        }
        Expediente borrado = obtener(id);
        if (!expedientes.eliminar(id)) {
            throw ApiException.noEncontrado("El expediente no existe.");
        }
        auditoria.registrarExpediente(yo, peticion.getRemoteAddr(), "ELIMINAR_EXPEDIENTE", id, etiqueta(borrado),
                null, null);
    }
}
