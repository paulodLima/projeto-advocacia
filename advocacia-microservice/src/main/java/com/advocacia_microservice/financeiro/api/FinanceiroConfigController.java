package com.advocacia_microservice.financeiro.api;

import com.advocacia_microservice.financeiro.application.FinanceiroConfigService;
import com.advocacia_microservice.financeiro.application.FinanceiroConfigService.Dados;
import com.advocacia_microservice.financeiro.domain.FinanceiroConfig.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/config/financeiro")
public class FinanceiroConfigController {
    public record Alterar<T>(@NotNull @Min(0) Long versao, @NotNull T dados) {}
    private final FinanceiroConfigService service;
    public FinanceiroConfigController(FinanceiroConfigService service) { this.service=service; }
    private UUID usuario(Principal p) { return UUID.fromString(p.getName()); }
    @GetMapping public Dados buscar(Principal p) { return service.buscar(usuario(p)); }
    @PostMapping("/contas") @ResponseStatus(HttpStatus.CREATED) public Dados criarConta(Principal p,@Valid @RequestBody Alterar<Conta> r) { return service.conta(usuario(p),r.versao(),null,r.dados()); }
    @PutMapping("/contas/{id}") public Dados conta(Principal p,@PathVariable UUID id,@Valid @RequestBody Alterar<Conta> r) { return service.conta(usuario(p),r.versao(),id,r.dados()); }
    @PostMapping("/categorias") @ResponseStatus(HttpStatus.CREATED) public Dados criarCategoria(Principal p,@Valid @RequestBody Alterar<Categoria> r) { return service.categoria(usuario(p),r.versao(),null,r.dados()); }
    @PutMapping("/categorias/{id}") public Dados categoria(Principal p,@PathVariable UUID id,@Valid @RequestBody Alterar<Categoria> r) { return service.categoria(usuario(p),r.versao(),id,r.dados()); }
    @PostMapping("/centros") @ResponseStatus(HttpStatus.CREATED) public Dados criarCentro(Principal p,@Valid @RequestBody Alterar<Centro> r) { return service.centro(usuario(p),r.versao(),null,r.dados()); }
    @PutMapping("/centros/{id}") public Dados centro(Principal p,@PathVariable UUID id,@Valid @RequestBody Alterar<Centro> r) { return service.centro(usuario(p),r.versao(),id,r.dados()); }
    @PostMapping("/custos") @ResponseStatus(HttpStatus.CREATED) public Dados custo(Principal p,@Valid @RequestBody Alterar<Custo> r) { return service.custo(usuario(p),r.versao(),r.dados()); }
    @DeleteMapping("/custos/{id}") public Dados excluirCusto(Principal p,@PathVariable UUID id,@RequestParam long versao) { return service.excluirCusto(usuario(p),versao,id); }
    @PutMapping("/urh") public Dados urh(Principal p,@Valid @RequestBody Alterar<Urh> r) { return service.urh(usuario(p),r.versao(),r.dados()); }
    @PutMapping("/capacidade") public Dados capacidade(Principal p,@Valid @RequestBody Alterar<Capacidade> r) { return service.capacidade(usuario(p),r.versao(),r.dados()); }
}
