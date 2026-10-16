package com.advocacia_microservice.agenda.api;

import com.advocacia_microservice.agenda.application.AgendaService;
import com.advocacia_microservice.agenda.domain.*;
import com.advocacia_microservice.agenda.api.request.*;
import com.advocacia_microservice.agenda.api.response.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/agenda")
public class AgendaController {
    private final AgendaService service;
    public AgendaController(AgendaService service) {this.service=service;}
    @GetMapping("/contatos") public List<OpcaoAgenda> contatos(Principal principal,@RequestParam(defaultValue="") String busca,@RequestParam(required=false) List<UUID> ids) {
        return service.contatos(usuario(principal),busca,ids==null?List.of():ids);
    }
    @GetMapping("/opcoes") public OpcoesAgendaResponse opcoes(Principal principal) {return OpcoesAgendaResponse.de(service.opcoes(usuario(principal)));}
    @GetMapping public PaginaAgendaResponse listar(Principal principal,@RequestParam LocalDate inicio,@RequestParam LocalDate fim,
            @RequestParam(required=false) TipoAtividade tipo,@RequestParam(required=false) SituacaoAgenda situacao,
            @RequestParam(required=false) UUID responsavel,@RequestParam(defaultValue="") String busca,
            @RequestParam(defaultValue="true") boolean incluirSemData,@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="50") int tamanho) {
        return PaginaAgendaResponse.de(service.listar(usuario(principal),new FiltroAgenda(inicio,fim,tipo,situacao,responsavel,busca,incluirSemData),pagina,tamanho));
    }
    @GetMapping("/calendario") public List<DiaAgendaResponse> calendario(Principal principal,@RequestParam LocalDate inicio,@RequestParam LocalDate fim,
            @RequestParam(required=false) TipoAtividade tipo,@RequestParam(required=false) SituacaoAgenda situacao,
            @RequestParam(required=false) UUID responsavel,@RequestParam(defaultValue="") String busca) {
        return service.calendario(usuario(principal),new FiltroAgenda(inicio,fim,tipo,situacao,responsavel,busca,false)).stream().map(DiaAgendaResponse::de).toList();
    }
    @GetMapping("/{id}") public AtividadeResponse buscar(Principal principal,@PathVariable UUID id) {return AtividadeResponse.de(service.buscar(usuario(principal),id));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public AtividadeResponse criar(Principal principal,@Valid @RequestBody SalvarAtividadeRequest request) {
        return AtividadeResponse.de(service.salvar(usuario(principal),null,request.comando()));
    }
    @PutMapping("/{id}") public AtividadeResponse editar(Principal principal,@PathVariable UUID id,@Valid @RequestBody SalvarAtividadeRequest request) {
        return AtividadeResponse.de(service.salvar(usuario(principal),id,request.comando()));
    }
    @PatchMapping("/{id}/situacao") public AtividadeResponse situacao(Principal principal,@PathVariable UUID id,@Valid @RequestBody AlterarSituacaoRequest request) {
        return AtividadeResponse.de(service.alterarSituacao(usuario(principal),id,request.comando()));
    }
    private UUID usuario(Principal principal) {return UUID.fromString(principal.getName());}
}
