package com.advocacia_microservice.usuario.api.response;

import com.advocacia_microservice.usuario.domain.StatusUsuario;
import com.advocacia_microservice.usuario.domain.Usuario;
import java.util.UUID;

public record UsuarioResponse(UUID id, String nome, String email, StatusUsuario status) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.id(), usuario.nome(), usuario.email(), usuario.status());
    }
}
