package com.advocacia_microservice.agenda.api;
import com.advocacia_microservice.agenda.application.ExecucoesWorkflowService;
import com.advocacia_microservice.agenda.api.request.*;
import com.advocacia_microservice.agenda.api.response.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/agenda/workflows")
public class ExecucoesWorkflowController {
    private final ExecucoesWorkflowService service;
    public ExecucoesWorkflowController(ExecucoesWorkflowService service){this.service=service;}
    @GetMapping public ResponseEntity<List<OpcaoWorkflowResponse>> modelos(Principal p,@RequestParam UUID tipoTarefaId){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.modelos(UUID.fromString(p.getName()),tipoTarefaId).stream().map(OpcaoWorkflowResponse::de).toList());
    }
    @PostMapping("/previsao") public PlanoWorkflowResponse prever(Principal p,@Valid @RequestBody PlanejarWorkflowRequest request){return PlanoWorkflowResponse.de(service.prever(UUID.fromString(p.getName()),request.comando()));}
    @PostMapping("/execucoes") public ExecucaoWorkflowResponse gerar(Principal p,@Valid @RequestBody GerarWorkflowRequest request){return ExecucaoWorkflowResponse.de(service.gerar(UUID.fromString(p.getName()),request.requisicaoId(),request.comando(),request.assinaturaPlano()));}
}
