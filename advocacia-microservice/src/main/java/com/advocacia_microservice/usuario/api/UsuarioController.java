package com.advocacia_microservice.usuario.api;

import com.advocacia_microservice.usuario.api.request.AtualizarUsuarioRequest;
import com.advocacia_microservice.usuario.api.request.CriarUsuarioRequest;
import com.advocacia_microservice.usuario.api.response.UsuarioResponse;
import com.advocacia_microservice.empresa.application.UsuariosDaEmpresaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuariosDaEmpresaService service;
    public UsuarioController(UsuariosDaEmpresaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(java.security.Principal principal, @Valid @RequestBody CriarUsuarioRequest request) {
        var usuario = service.criar(UUID.fromString(principal.getName()), request.nome(), request.email());
        return ResponseEntity.created(URI.create("/api/usuarios/" + usuario.id()))
                .body(UsuarioResponse.de(usuario));
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(java.security.Principal principal, @PathVariable UUID id) {
        return UsuarioResponse.de(service.buscar(UUID.fromString(principal.getName()), id));
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(java.security.Principal principal, @PathVariable UUID id,
                                    @Valid @RequestBody AtualizarUsuarioRequest request) {
        return UsuarioResponse.de(service.atualizar(UUID.fromString(principal.getName()), id, request.nome(), request.email(), request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(java.security.Principal principal, @PathVariable UUID id) {
        service.excluir(UUID.fromString(principal.getName()), id);
        return ResponseEntity.noContent().build();
    }
}
