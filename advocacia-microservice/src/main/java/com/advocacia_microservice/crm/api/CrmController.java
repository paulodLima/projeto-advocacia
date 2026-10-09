package com.advocacia_microservice.crm.api;

import com.advocacia_microservice.crm.application.CrmService;
import com.advocacia_microservice.crm.domain.Lead;
import java.security.Principal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/crm")
public class CrmController {
    private final CrmService service;
    public CrmController(CrmService service) {this.service=service;}
    private UUID usuario(Principal p) {return UUID.fromString(p.getName());}
    private <T> ResponseEntity<T> ok(T valor) {return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(valor);}
    @GetMapping("/opcoes") public ResponseEntity<?> opcoes(Principal p) {return ok(service.opcoes(usuario(p)));}
    @GetMapping("/leads") public ResponseEntity<?> listar(Principal p,@RequestParam(defaultValue="") String busca,@RequestParam(defaultValue="ativo") String status,@RequestParam(defaultValue="") String temperatura,@RequestParam(defaultValue="") String origem,@RequestParam(defaultValue="false") boolean atrasados,@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="24") int tamanho) {return ok(service.listar(usuario(p),busca,status,temperatura,origem,atrasados,pagina,tamanho));}
    @GetMapping("/leads/{id}") public ResponseEntity<?> buscar(Principal p,@PathVariable UUID id) {return ok(service.buscar(usuario(p),id));}
    @PostMapping("/leads") public ResponseEntity<?> criar(Principal p,@RequestBody CrmService.Formulario f) {return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(service.salvar(usuario(p),null,f));}
    @PutMapping("/leads/{id}") public ResponseEntity<?> salvar(Principal p,@PathVariable UUID id,@RequestBody CrmService.Formulario f) {return ok(service.salvar(usuario(p),id,f));}
    @PutMapping("/leads/{id}/estado") public ResponseEntity<?> estado(Principal p,@PathVariable UUID id,@RequestBody CrmService.Atualizacao f) {return ok(service.estado(usuario(p),id,f));}
    @PostMapping("/leads/{id}/contatos") public ResponseEntity<?> registrar(Principal p,@PathVariable UUID id,@RequestBody CrmService.ContatoRequest f) {return ok(service.registrar(usuario(p),id,f));}
    @PostMapping("/leads/{id}/comentarios") public ResponseEntity<?> comentar(Principal p,@PathVariable UUID id,@RequestBody CrmService.ComentarioRequest f) {return ok(service.comentar(usuario(p),id,f));}
    @PostMapping("/leads/{id}/converter") public ResponseEntity<?> converter(Principal p,@PathVariable UUID id,@RequestBody CrmService.ConversaoRequest f) {return ok(service.converter(usuario(p),id,f));}
    @DeleteMapping("/leads/{id}") public ResponseEntity<?> excluir(Principal p,@PathVariable UUID id,@RequestParam int versao) {service.excluir(usuario(p),id,versao);return ResponseEntity.noContent().build();}
    @PutMapping("/cadencia") public ResponseEntity<?> cadencia(Principal p,@RequestBody List<Lead.Passo> passos) {return ok(service.salvarCadencia(usuario(p),passos));}
    @GetMapping("/campanhas") public ResponseEntity<?> campanhas(Principal p,@RequestParam int ano) {return ok(service.marketing(usuario(p),ano));}
    @PostMapping("/campanhas") public ResponseEntity<?> campanha(Principal p,@RequestBody CrmService.CampanhaRequest f) {service.salvarCampanha(usuario(p),null,f);return ResponseEntity.status(201).build();}
    @PutMapping("/campanhas/{id}") public ResponseEntity<?> campanha(Principal p,@PathVariable UUID id,@RequestBody CrmService.CampanhaRequest f) {service.salvarCampanha(usuario(p),id,f);return ResponseEntity.noContent().build();}
    @DeleteMapping("/campanhas/{id}") public ResponseEntity<?> excluirCampanha(Principal p,@PathVariable UUID id,@RequestParam int versao) {service.excluirCampanha(usuario(p),id,versao);return ResponseEntity.noContent().build();}
    @PutMapping("/campanhas/{id}/abordagens") public ResponseEntity<?> abordar(Principal p,@PathVariable UUID id,@RequestBody CrmService.AbordagemRequest f) {service.abordar(usuario(p),id,f);return ResponseEntity.noContent().build();}
    @PostMapping("/manutencoes/{id}/contato") public ResponseEntity<?> manutencao(Principal p,@PathVariable UUID id) {service.registrarManutencao(usuario(p),id);return ResponseEntity.noContent().build();}
    @GetMapping("/notificacoes") public ResponseEntity<?> notificacoes(Principal p) {return ok(service.notificacoes(usuario(p)));}
    @PutMapping("/notificacoes/{id}/lida") public ResponseEntity<?> ler(Principal p,@PathVariable UUID id) {service.lerNotificacao(usuario(p),id);return ResponseEntity.noContent().build();}
}
