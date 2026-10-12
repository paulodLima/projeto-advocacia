package com.advocacia_microservice.plataforma.api;

import com.advocacia_microservice.plataforma.api.request.ConvidarResponsavelRequest;
import com.advocacia_microservice.plataforma.api.response.ConviteInicialResponse;
import com.advocacia_microservice.plataforma.application.ConvitesIniciaisService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plataforma/admin/convites")
public class ConvitesIniciaisController {
    private final ConvitesIniciaisService service;
    public ConvitesIniciaisController(ConvitesIniciaisService service) { this.service = service; }
    @GetMapping public List<ConviteInicialResponse> listar(Principal autor) {
        return service.listar(UUID.fromString(autor.getName())).stream().map(ConviteInicialResponse::de).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ConviteInicialResponse convidar(Principal autor, @Valid @RequestBody ConvidarResponsavelRequest request) {
        return ConviteInicialResponse.de(service.convidar(UUID.fromString(autor.getName()), request.nome(), request.email()));
    }
    @PostMapping("/{id}/reenviar") public ConviteInicialResponse reenviar(Principal autor, @PathVariable UUID id) {
        return ConviteInicialResponse.de(service.reenviar(UUID.fromString(autor.getName()), id));
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(Principal autor, @PathVariable UUID id) { service.cancelar(UUID.fromString(autor.getName()), id); }
}
