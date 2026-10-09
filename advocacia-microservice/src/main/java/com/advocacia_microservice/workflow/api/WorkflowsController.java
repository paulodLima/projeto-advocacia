package com.advocacia_microservice.workflow.api;
import com.advocacia_microservice.workflow.application.WorkflowsService;
import com.advocacia_microservice.workflow.application.WorkflowsService.Etapa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api/config/workflows")
public class WorkflowsController {
    public record Criar(@NotBlank @Size(max=150) String nome) {}
    public record Salvar(@NotBlank @Size(max=150) String nome,@NotNull @Min(0) @Max(365) Integer buffer,@NotNull @Min(0) Integer versao,@NotNull @Size(max=100) List<@NotNull UUID> gatilhos,@NotNull @Size(max=100) List<@NotNull Etapa> etapas) {}
    private final WorkflowsService service;
    public WorkflowsController(WorkflowsService service){this.service=service;}
    @GetMapping public WorkflowsService.Dados buscar(Principal p){return service.buscar(UUID.fromString(p.getName()));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public WorkflowsService.Workflow criar(Principal p,@Valid @RequestBody Criar body){return service.criar(UUID.fromString(p.getName()),body.nome());}
    @PutMapping("/{id}") public WorkflowsService.Workflow salvar(Principal p,@PathVariable UUID id,@Valid @RequestBody Salvar body){return service.salvar(UUID.fromString(p.getName()),id,body.nome(),body.buffer(),body.versao(),body.gatilhos(),body.etapas());}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void excluir(Principal p,@PathVariable UUID id,@RequestParam int versao){service.excluir(UUID.fromString(p.getName()),id,versao);}
}

