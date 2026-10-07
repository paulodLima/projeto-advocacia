package com.advocacia_microservice.cofre.api;

import com.advocacia_microservice.cofre.application.CofreService;
import java.security.Principal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/config/cofre")
public class CofreController {
    private final CofreService service;
    public CofreController(CofreService service) { this.service=service; }
    private UUID usuario(Principal p) { return UUID.fromString(p.getName()); }
    // Todas as respostas do cofre, inclusive metadados, não devem ser armazenadas em cache.
    private <T> ResponseEntity<T> resposta(T body,HttpStatus status) { return ResponseEntity.status(status).header("Cache-Control","no-store").header("Pragma","no-cache").body(body); }
    @GetMapping public ResponseEntity<CofreService.Dados> listar(Principal p) { return resposta(service.buscar(usuario(p)),HttpStatus.OK); }
    @PostMapping public ResponseEntity<CofreService.Credencial> criar(Principal p,@RequestBody CofreService.Formulario f) { return resposta(service.salvar(usuario(p),null,f),HttpStatus.CREATED); }
    @PutMapping("/{id}") public ResponseEntity<CofreService.Credencial> editar(Principal p,@PathVariable UUID id,@RequestBody CofreService.Formulario f) { return resposta(service.salvar(usuario(p),id,f),HttpStatus.OK); }
    @PostMapping("/{id}/revelar") public ResponseEntity<Map<String,String>> revelar(Principal p,@PathVariable UUID id) { return resposta(Map.of("senha",service.revelar(usuario(p),id)),HttpStatus.OK); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(Principal p,@PathVariable UUID id,@RequestParam int versao) { service.excluir(usuario(p),id,versao);return resposta(null,HttpStatus.NO_CONTENT); }
}
