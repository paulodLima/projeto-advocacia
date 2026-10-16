package com.advocacia_microservice.agenda.api;
import com.advocacia_microservice.agenda.application.LembretesAgendaService;
import com.advocacia_microservice.agenda.api.request.*;
import com.advocacia_microservice.agenda.api.response.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/agenda/lembretes")
public class LembretesAgendaController {
    private final LembretesAgendaService service;
    public LembretesAgendaController(LembretesAgendaService service) {this.service=service;}
    @GetMapping public ResponseEntity<PainelLembretesResponse> listar(Principal principal,@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(PainelLembretesResponse.de(service.listar(UUID.fromString(principal.getName()),pagina,tamanho)));
    }
    @PutMapping("/preferencias") public PreferenciasLembreteResponse preferencias(Principal principal,@Valid @RequestBody PreferenciasLembreteRequest request) {
        return PreferenciasLembreteResponse.de(service.salvarPreferencias(UUID.fromString(principal.getName()),request.dominio()));
    }
    @PatchMapping("/{atividadeId}/lido") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ler(Principal principal,@PathVariable UUID atividadeId,@Valid @RequestBody LerLembreteRequest request) {
        service.ler(UUID.fromString(principal.getName()),atividadeId,request.comando());
    }
}
