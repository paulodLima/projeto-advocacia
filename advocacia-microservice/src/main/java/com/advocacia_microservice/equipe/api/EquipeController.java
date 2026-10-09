package com.advocacia_microservice.equipe.api;

import com.advocacia_microservice.equipe.application.EquipeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/equipe")
public class EquipeController {
    public record ConviteRequest(@NotBlank @Size(max = 150) String nome,
            @NotBlank @Email @Size(max = 254) String email, @NotNull @Valid ConfiguracaoMembroRequest configuracao) {}
    private final EquipeService service;
    public EquipeController(EquipeService service) { this.service = service; }
    @GetMapping("/me") public EquipeService.Acesso acesso(Principal principal) { return service.acesso(id(principal)); }
    @GetMapping public EquipeService.Equipe listar(Principal principal) { return service.listar(id(principal)); }
    @PutMapping("/membros/{id}") public EquipeService.Membro atualizar(Principal principal, @PathVariable UUID id, @Valid @RequestBody ConfiguracaoMembroRequest request) {
        return service.atualizar(id(principal), id, request);
    }
    @PostMapping("/convites") @ResponseStatus(HttpStatus.CREATED)
    public EquipeService.Convite convidar(Principal principal, @Valid @RequestBody ConviteRequest request) {
        return service.convidar(id(principal), request.nome(), request.email(), request.configuracao());
    }
    @DeleteMapping("/convites/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(Principal principal, @PathVariable UUID id) { service.cancelar(id(principal), id); }
    @PostMapping("/convites/{id}/reenviar")
    public EquipeService.Convite reenviar(Principal principal, @PathVariable UUID id) { return service.reenviar(id(principal), id); }
    private UUID id(Principal principal) { return UUID.fromString(principal.getName()); }
}
