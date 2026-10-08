package com.advocacia_microservice.cliente.api;

import com.advocacia_microservice.cliente.application.ContatosService;
import com.advocacia_microservice.cliente.domain.Contato;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/contatos")
public class ContatosController {
    private final ContatosService service;
    public ContatosController(ContatosService service) {this.service=service;}
    private UUID usuario(Principal p) {return UUID.fromString(p.getName());}
    private <T> ResponseEntity<T> resposta(T body,HttpStatus status) {return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);}
    @GetMapping public ResponseEntity<ContatosService.Pagina> listar(Principal p,@RequestParam(defaultValue="") String busca,@RequestParam(defaultValue="") String tipo,@RequestParam(required=false) UUID indicador,@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="24") int tamanho) {return resposta(service.listar(usuario(p),busca,tipo,indicador,pagina,tamanho),HttpStatus.OK);}
    @GetMapping("/opcoes") public ResponseEntity<ContatosService.Opcoes> opcoes(Principal p) {return resposta(service.opcoes(usuario(p)),HttpStatus.OK);}
    @GetMapping("/{id}") public ResponseEntity<Contato> buscar(Principal p,@PathVariable UUID id) {return resposta(service.buscar(usuario(p),id),HttpStatus.OK);}
    @PostMapping public ResponseEntity<Contato> criar(Principal p,@RequestBody ContatosService.Formulario f) {return resposta(service.salvar(usuario(p),null,f),HttpStatus.CREATED);}
    @PutMapping("/{id}") public ResponseEntity<Contato> editar(Principal p,@PathVariable UUID id,@RequestBody ContatosService.Formulario f) {return resposta(service.salvar(usuario(p),id,f),HttpStatus.OK);}
    @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(Principal p,@PathVariable UUID id,@RequestParam int versao) {service.excluir(usuario(p),id,versao);return resposta(null,HttpStatus.NO_CONTENT);}
    public record NovoIndicador(String nome) {}
    @PostMapping("/indicadores") public ResponseEntity<ContatosService.Indicador> indicador(Principal p,@RequestBody NovoIndicador f) {return resposta(service.criarIndicador(usuario(p),f.nome()),HttpStatus.CREATED);}
}
