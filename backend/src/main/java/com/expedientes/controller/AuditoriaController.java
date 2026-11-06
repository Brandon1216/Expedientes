package com.expedientes.controller;

import com.expedientes.config.ApiException;
import com.expedientes.dto.Dtos.Movimiento;
import com.expedientes.dto.Dtos.Pagina;
import com.expedientes.dto.Dtos.ResumenAuditoria;
import com.expedientes.dto.Dtos.SesionRegistro;
import com.expedientes.model.Usuario;
import com.expedientes.repository.AuditoriaRepository;
import com.expedientes.repository.AuditoriaRepository.FiltroMovimientos;
import com.expedientes.repository.AuditoriaRepository.FiltroSesiones;
import com.expedientes.security.AuthInterceptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private static final int MAXIMO_POR_PAGINA = 5000;

    private final AuditoriaRepository auditoria;

    public AuditoriaController(AuditoriaRepository auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping("/movimientos")
    public Pagina<Movimiento> movimientos(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                                          @RequestParam(required = false) String usuario,
                                          @RequestParam(required = false) String accion,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(required = false) String desde,
                                          @RequestParam(required = false) String hasta,
                                          @RequestParam(defaultValue = "false") boolean ocultarSesiones,
                                          @RequestParam(defaultValue = "0") int pagina,
                                          @RequestParam(defaultValue = "50") int tamano) {
        soloAdmin(yo);
        int t = tamano(tamano);
        return auditoria.movimientos(
                new FiltroMovimientos(usuario, accion, q, fecha(desde), fecha(hasta), ocultarSesiones),
                (long) Math.max(0, pagina) * t, t);
    }

    @GetMapping("/resumen")
    public ResumenAuditoria resumen(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                                    @RequestParam(required = false) String usuario,
                                    @RequestParam(required = false) String accion,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(required = false) String desde,
                                    @RequestParam(required = false) String hasta,
                                    @RequestParam(defaultValue = "false") boolean ocultarSesiones) {
        soloAdmin(yo);
        return auditoria.resumen(
                new FiltroMovimientos(usuario, accion, q, fecha(desde), fecha(hasta), ocultarSesiones));
    }

    @GetMapping("/sesiones")
    public Pagina<SesionRegistro> sesiones(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                                           @RequestParam(required = false) String usuario,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(required = false) String desde,
                                           @RequestParam(required = false) String hasta,
                                           @RequestParam(defaultValue = "0") int pagina,
                                           @RequestParam(defaultValue = "50") int tamano) {
        soloAdmin(yo);
        int t = tamano(tamano);
        return auditoria.sesiones(new FiltroSesiones(usuario, q, fecha(desde), fecha(hasta)),
                (long) Math.max(0, pagina) * t, t);
    }

    @GetMapping("/usuarios")
    public List<String> usuarios(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo) {
        soloAdmin(yo);
        return auditoria.usuarios();
    }

    private static int tamano(int valor) {
        return Math.min(MAXIMO_POR_PAGINA, Math.max(1, valor));
    }

    private static String fecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim()).toString();
        } catch (DateTimeParseException e) {
            throw ApiException.invalido("La fecha debe tener el formato AAAA-MM-DD.");
        }
    }

    private static void soloAdmin(Usuario yo) {
        if (!yo.esAdmin()) {
            throw ApiException.prohibido("Solo un administrador puede hacer esto.");
        }
    }
}
