package com.advocacia_microservice.parceiro.api;

import com.advocacia_microservice.parceiro.application.ParceirosService;
import com.advocacia_microservice.parceiro.domain.Parceiro;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parceiros")
public class ParceirosController {
    private final ParceirosService service;
    public ParceirosController(ParceirosService service) { this.service = service; }
    private UUID usuario(Principal p) { return UUID.fromString(p.getName()); }
    private <T> ResponseEntity<T> resposta(T valor) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(valor); }
    @GetMapping public ResponseEntity<ParceirosService.Pagina> listar(Principal p, @RequestParam(defaultValue = "") String busca,
            @RequestParam(defaultValue = "") String uf, @RequestParam(defaultValue = "") String area,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "24") int tamanho) {
        return resposta(service.listar(usuario(p), busca, uf, area, pagina, tamanho));
    }
    @GetMapping("/opcoes") public ResponseEntity<ParceirosService.Opcoes> opcoes(Principal p) { return resposta(service.opcoes(usuario(p))); }
    @GetMapping("/{id}") public ResponseEntity<Parceiro> buscar(Principal p, @PathVariable UUID id) { return resposta(service.buscar(usuario(p), id)); }
    @GetMapping("/{id}/carteira") public ResponseEntity<ParceirosService.Carteira> carteira(Principal p, @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "24") int tamanho) {
        return resposta(service.carteira(usuario(p), id, pagina, tamanho));
    }
    @PostMapping public ResponseEntity<Parceiro> criar(Principal p, @RequestBody ParceirosService.Formulario f) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(service.salvar(usuario(p), null, f));
    }
    @PutMapping("/{id}") public ResponseEntity<Parceiro> atualizar(Principal p, @PathVariable UUID id, @RequestBody ParceirosService.Formulario f) {
        return resposta(service.salvar(usuario(p), id, f));
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(Principal p, @PathVariable UUID id, @RequestParam int versao) {
        service.excluir(usuario(p), id, versao); return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
