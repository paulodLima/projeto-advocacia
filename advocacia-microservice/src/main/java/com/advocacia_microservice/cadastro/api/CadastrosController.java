package com.advocacia_microservice.cadastro.api;

import com.advocacia_microservice.cadastro.application.CadastrosService;
import com.advocacia_microservice.cadastro.domain.*;
import com.advocacia_microservice.cadastro.infrastructure.CadastrosRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/config/cadastros")
public class CadastrosController {
    public record RegistroRequest(@NotNull @Size(max = 4) Map<@NotNull @Size(max = 30) String, @NotNull @Size(max = 150) String> campos, @NotNull Boolean ativo) {}
    public record RotinaRequest(String nome, String periodo) {}
    public record SistemasRequest(@NotNull @Size(max = 30) List<@NotNull Sistema> sistemas) {}
    public record AtalhosRequest(@NotNull @Size(max = 14) List<@NotNull String> atalhos) {}
    private final CadastrosService service;
    public CadastrosController(CadastrosService service) { this.service = service; }
    @GetMapping public CadastrosService.Resposta buscar(Principal p) { return service.buscar(UUID.fromString(p.getName())); }
    @GetMapping("/inicio") public CadastrosService.Inicio inicio(Principal p) { return service.inicio(UUID.fromString(p.getName())); }
    @PutMapping("/atalhos") public List<String> atalhos(Principal p, @Valid @RequestBody AtalhosRequest body) { return service.salvarAtalhos(UUID.fromString(p.getName()), body.atalhos()); }
    @PostMapping("/listas/{tipo}") @ResponseStatus(HttpStatus.CREATED)
    public CadastroRegistro criar(Principal p, @PathVariable String tipo, @Valid @RequestBody RegistroRequest body) { return service.salvar(UUID.fromString(p.getName()), tipo, null, body.campos(), body.ativo()); }
    @PutMapping("/listas/{tipo}/{id}")
    public CadastroRegistro salvar(Principal p, @PathVariable String tipo, @PathVariable UUID id, @Valid @RequestBody RegistroRequest body) { return service.salvar(UUID.fromString(p.getName()), tipo, id, body.campos(), body.ativo()); }
    @DeleteMapping("/listas/{tipo}/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(Principal p, @PathVariable String tipo, @PathVariable UUID id) { service.excluir(UUID.fromString(p.getName()), tipo, id); }
    @PostMapping("/rotinas") @ResponseStatus(HttpStatus.CREATED)
    public CadastrosRepository.Rotina criarRotina(Principal p, @RequestBody RotinaRequest body) { return service.criarRotina(UUID.fromString(p.getName()), body.nome(), body.periodo()); }
    @DeleteMapping("/rotinas/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirRotina(Principal p, @PathVariable UUID id) { service.excluirRotina(UUID.fromString(p.getName()), id); }
    @PutMapping("/sistemas") public List<Sistema> sistemas(Principal p, @Valid @RequestBody SistemasRequest body) { return service.salvarSistemas(UUID.fromString(p.getName()), body.sistemas()); }
}
