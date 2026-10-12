package com.advocacia_microservice.usuario.api;
import com.advocacia_microservice.usuario.api.request.DadosRequest;
import com.advocacia_microservice.usuario.api.request.FotoRequest;

import com.advocacia_microservice.usuario.application.MeuPerfilService;
import com.advocacia_microservice.usuario.domain.UsuarioPerfil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios/me/perfil")
public class MeuPerfilController {


    private final MeuPerfilService service;
    public MeuPerfilController(MeuPerfilService service) { this.service = service; }

    @GetMapping
    public UsuarioPerfil buscar(Principal principal) {
        return service.buscar(UUID.fromString(principal.getName()));
    }
    @PutMapping
    public UsuarioPerfil atualizar(Principal principal, @Valid @RequestBody DadosRequest request) {
        return service.atualizar(UUID.fromString(principal.getName()), request.telefone(), request.emailPessoal(), request.endereco());
    }
    @PutMapping("/foto")
    public UsuarioPerfil foto(Principal principal, @Valid @RequestBody FotoRequest request) {
        return service.atualizarFoto(UUID.fromString(principal.getName()), request.foto());
    }
    @DeleteMapping("/foto")
    public UsuarioPerfil removerFoto(Principal principal) {
        return service.atualizarFoto(UUID.fromString(principal.getName()), "");
    }
}
