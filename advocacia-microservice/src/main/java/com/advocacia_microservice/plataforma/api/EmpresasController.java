package com.advocacia_microservice.plataforma.api;

import com.advocacia_microservice.plataforma.api.request.SalvarEmpresaRequest;
import com.advocacia_microservice.plataforma.api.response.EmpresaResponse;
import com.advocacia_microservice.plataforma.api.response.ListaEmpresasResponse;
import com.advocacia_microservice.plataforma.application.EmpresasService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plataforma/admin/empresas")
public class EmpresasController {
    private final EmpresasService empresas;
    public EmpresasController(EmpresasService empresas) { this.empresas = empresas; }
    @GetMapping
    public ListaEmpresasResponse listar(@RequestParam(defaultValue = "0") int pagina) {
        return ListaEmpresasResponse.de(empresas.listar(pagina));
    }
    @GetMapping("/{id}")
    public EmpresaResponse ficha(@PathVariable UUID id) { return EmpresaResponse.de(empresas.buscar(id)); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaResponse criar(@Valid @RequestBody SalvarEmpresaRequest dados, Principal autor) {
        return EmpresaResponse.de(empresas.criar(dados.paraComando(), UUID.fromString(autor.getName())));
    }
    @PutMapping("/{id}")
    public EmpresaResponse editar(@PathVariable UUID id, @Valid @RequestBody SalvarEmpresaRequest dados, Principal autor) {
        return EmpresaResponse.de(empresas.editar(id, dados.paraComando(), UUID.fromString(autor.getName())));
    }
}
