package com.advocacia_microservice.empresa.api;

import com.advocacia_microservice.empresa.application.EmpresaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {
    public record EmpresaRequest(@NotNull @Size(max = 24)
            Map<@NotNull @Size(max = 40) String, @NotNull @Size(max = 1000) String> dados) {}
    private final EmpresaService service;
    public EmpresaController(EmpresaService service) { this.service = service; }
    @GetMapping("/minha")
    public EmpresaService.MinhaEmpresa buscar(Principal principal) { return service.buscar(UUID.fromString(principal.getName())); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaService.MinhaEmpresa criar(Principal principal, @Valid @RequestBody EmpresaRequest request) {
        return service.criar(UUID.fromString(principal.getName()), request.dados());
    }
    @PutMapping("/minha")
    public EmpresaService.MinhaEmpresa atualizar(Principal principal, @Valid @RequestBody EmpresaRequest request) {
        return service.atualizar(UUID.fromString(principal.getName()), request.dados());
    }
}
