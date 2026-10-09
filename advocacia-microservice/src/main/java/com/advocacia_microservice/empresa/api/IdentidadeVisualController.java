package com.advocacia_microservice.empresa.api;

import com.advocacia_microservice.empresa.application.IdentidadeVisualService;
import com.advocacia_microservice.empresa.domain.IdentidadeVisual;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class IdentidadeVisualController {
    private final IdentidadeVisualService service;
    public IdentidadeVisualController(IdentidadeVisualService service) { this.service = service; }
    @GetMapping("/api/empresas/minha/identidade")
    public ResponseEntity<IdentidadeVisualService.Resposta> buscar(Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.buscar(UUID.fromString(principal.getName())));
    }
    @PutMapping("/api/empresas/minha/identidade")
    public IdentidadeVisualService.Resposta salvar(Principal principal, @RequestBody IdentidadeVisual identidade) {
        return service.salvar(UUID.fromString(principal.getName()), identidade);
    }
    // Somente informações visuais públicas; este identificador nunca determina o vínculo do usuário.
    @GetMapping("/api/public/empresas/{empresaId}/identidade")
    public ResponseEntity<IdentidadeVisualService.Resposta> publica(@PathVariable UUID empresaId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.publica(empresaId));
    }
}
