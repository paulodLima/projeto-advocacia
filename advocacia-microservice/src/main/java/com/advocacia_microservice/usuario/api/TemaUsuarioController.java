package com.advocacia_microservice.usuario.api;

import com.advocacia_microservice.usuario.application.TemaUsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/usuarios/me/tema")
public class TemaUsuarioController {
    public record Alterar(@NotBlank @Size(max=10) String tema) {}
    private final TemaUsuarioService service;
    public TemaUsuarioController(TemaUsuarioService service) { this.service=service; }
    @GetMapping public TemaUsuarioService.Preferencia buscar(Principal principal) { return service.buscar(UUID.fromString(principal.getName())); }
    @PutMapping public TemaUsuarioService.Preferencia salvar(Principal principal,@Valid @RequestBody Alterar request) { return service.salvar(UUID.fromString(principal.getName()),request.tema()); }
}
