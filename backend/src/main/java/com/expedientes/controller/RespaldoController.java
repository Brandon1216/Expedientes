package com.expedientes.controller;

import com.expedientes.config.ApiException;
import com.expedientes.config.CopiaUsb;
import com.expedientes.dto.Dtos.AvisoRespaldo;
import com.expedientes.dto.Dtos.CopiaHecha;
import com.expedientes.dto.Dtos.CopiarUsbRequest;
import com.expedientes.dto.Dtos.EstadoRespaldos;
import com.expedientes.model.Usuario;
import com.expedientes.security.AuthInterceptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/respaldos")
public class RespaldoController {

    private static final int DIAS_PARA_AVISAR = 7;

    private final CopiaUsb copiaUsb;

    public RespaldoController(CopiaUsb copiaUsb) {
        this.copiaUsb = copiaUsb;
    }

    @GetMapping("/aviso")
    public AvisoRespaldo aviso(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo) {
        soloAdmin(yo);
        Integer dias = copiaUsb.diasDesdeUltima();
        return new AvisoRespaldo(dias, dias == null || dias >= DIAS_PARA_AVISAR);
    }

    @GetMapping("/estado")
    public EstadoRespaldos estado(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo) {
        soloAdmin(yo);
        return new EstadoRespaldos(copiaUsb.unidades(), copiaUsb.historial());
    }

    @PostMapping("/copiar")
    public CopiaHecha copiar(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario yo,
                             @RequestBody CopiarUsbRequest req) {
        soloAdmin(yo);
        return copiaUsb.copiar(req.unidad(), yo.nombre());
    }

    private static void soloAdmin(Usuario yo) {
        if (!yo.esAdmin()) {
            throw ApiException.prohibido("Solo un administrador puede hacer esto.");
        }
    }
}
