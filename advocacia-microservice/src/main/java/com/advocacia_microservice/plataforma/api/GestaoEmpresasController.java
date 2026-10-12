package com.advocacia_microservice.plataforma.api;
import com.advocacia_microservice.plataforma.application.GestaoEmpresasService;
import com.advocacia_microservice.plataforma.api.request.AlterarContratoRequest;
import com.advocacia_microservice.plataforma.api.response.GestaoEmpresaResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/plataforma/admin/gestao-empresas")
public class GestaoEmpresasController {
    private final GestaoEmpresasService service;
    public GestaoEmpresasController(GestaoEmpresasService service) { this.service = service; }
    @GetMapping public List<GestaoEmpresaResponse> listar(@RequestParam(defaultValue="") String busca,
            @RequestParam(defaultValue="") String situacao, @RequestParam(defaultValue="0") int pagina) {
        return service.listar(busca,situacao,pagina).stream().map(GestaoEmpresaResponse::de).toList();
    }
    @GetMapping("/{id}") public GestaoEmpresaResponse buscar(@PathVariable UUID id) { return GestaoEmpresaResponse.de(service.buscar(id)); }
    @PutMapping("/{id}") public GestaoEmpresaResponse alterar(@PathVariable UUID id, @Valid @RequestBody AlterarContratoRequest request, Principal principal) {
        return GestaoEmpresaResponse.de(service.alterar(id,request.comando(),UUID.fromString(principal.getName())));
    }
}
