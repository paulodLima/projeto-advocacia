package com.advocacia_microservice.caso.api;
import com.advocacia_microservice.caso.application.FinanceiroCasoService;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/casos/{caso}/financeiro")
public class FinanceiroCasoController {
    private final FinanceiroCasoService service;
    public FinanceiroCasoController(FinanceiroCasoService service){this.service=service;}
    private UUID usuario(Principal p){return UUID.fromString(p.getName());}
    @ModelAttribute public void cache(HttpServletResponse r){r.setHeader("Cache-Control","no-store");}
    @GetMapping public Object buscar(Principal p,@PathVariable UUID caso){return service.buscar(usuario(p),caso);}
    @GetMapping("/opcoes") public Object opcoes(Principal p,@PathVariable UUID caso){return service.opcoes(usuario(p),caso);}
    @PostMapping("/honorarios") @ResponseStatus(HttpStatus.CREATED) public Object honorario(Principal p,@PathVariable UUID caso,@RequestBody FinanceiroCasoService.HonorarioRequest f){return service.honorario(usuario(p),caso,f);}
    @DeleteMapping("/honorarios/{id}") public Object excluirHonorario(Principal p,@PathVariable UUID caso,@PathVariable UUID id,@RequestParam long versao){return service.excluirHonorario(usuario(p),caso,id,versao);}
    @PostMapping("/lancamentos") @ResponseStatus(HttpStatus.CREATED) public Object lancamento(Principal p,@PathVariable UUID caso,@RequestBody FinanceiroCasoService.LancamentoRequest f){return service.lancamento(usuario(p),caso,f);}
    @PutMapping("/lancamentos/{id}/baixa") public Object baixa(Principal p,@PathVariable UUID caso,@PathVariable UUID id,@RequestBody FinanceiroCasoService.Baixa f){return service.baixa(usuario(p),caso,id,f);}
    @PutMapping("/lancamentos/{id}/vencimento") public Object vencimento(Principal p,@PathVariable UUID caso,@PathVariable UUID id,@RequestBody FinanceiroCasoService.Vencimento f){return service.vencimento(usuario(p),caso,id,f);}
    @PutMapping("/lancamentos/{id}/titularidade") public Object titularidade(Principal p,@PathVariable UUID caso,@PathVariable UUID id,@RequestBody FinanceiroCasoService.Titularidade f){return service.titularidade(usuario(p),caso,id,f);}
    @DeleteMapping("/lancamentos/{id}") public Object excluirLancamento(Principal p,@PathVariable UUID caso,@PathVariable UUID id,@RequestParam long versao){return service.excluirLancamento(usuario(p),caso,id,versao);}
    @PostMapping("/despesas") @ResponseStatus(HttpStatus.CREATED) public Object despesa(Principal p,@PathVariable UUID caso,@RequestBody FinanceiroCasoService.DespesaRequest f){return service.despesa(usuario(p),caso,f);}
    @PutMapping("/despesas/{id}/reembolso") public Object reembolso(Principal p,@PathVariable UUID caso,@PathVariable UUID id,@RequestBody FinanceiroCasoService.Reembolso f){return service.reembolso(usuario(p),caso,id,f);}
    @DeleteMapping("/despesas/{id}") public Object excluirDespesa(Principal p,@PathVariable UUID caso,@PathVariable UUID id,@RequestParam long versao){return service.excluirDespesa(usuario(p),caso,id,versao);}
}
