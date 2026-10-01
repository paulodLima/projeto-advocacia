package com.advocacia_microservice.usuario.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record Usuario(UUID id, String nome, String email, StatusUsuario status) {
    public Usuario {
        Objects.requireNonNull(id, "Id é obrigatório");
        Objects.requireNonNull(status, "Status é obrigatório");
        if (nome == null || nome.isBlank() || nome.strip().length() > 150) {
            throw new IllegalArgumentException("Nome deve ter entre 1 e 150 caracteres");
        }
        if (email == null || email.isBlank() || email.strip().length() > 254) {
            throw new IllegalArgumentException("Email deve ter entre 1 e 254 caracteres");
        }
        nome = nome.strip();
        email = email.strip().toLowerCase(Locale.ROOT);
    }

    public static Usuario criar(String nome, String email) {
        return new Usuario(UUID.randomUUID(), nome, email, StatusUsuario.ATIVO);
    }

    public Usuario atualizar(String nome, String email, StatusUsuario status) {
        return new Usuario(id, nome, email, status);
    }
}
