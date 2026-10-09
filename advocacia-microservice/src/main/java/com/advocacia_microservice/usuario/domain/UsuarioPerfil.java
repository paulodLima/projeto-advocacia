package com.advocacia_microservice.usuario.domain;

import java.util.UUID;

public record UsuarioPerfil(UUID usuarioId, String telefone, String emailPessoal, String endereco, String foto) {
    public static UsuarioPerfil vazio(UUID id) {
        return new UsuarioPerfil(id, "", "", "", "");
    }
}
