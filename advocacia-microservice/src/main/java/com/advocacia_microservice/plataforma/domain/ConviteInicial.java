package com.advocacia_microservice.plataforma.domain;

import java.time.Instant;
import java.util.UUID;

public record ConviteInicial(UUID id, String nome, String email, String status, Instant expiraEm,
                            UUID usuarioId, UUID empresaId) {
    public boolean permiteEntrada(Instant agora) {
        return (status.equals("PENDENTE") || status.equals("ACEITO")) && expiraEm.isAfter(agora);
    }
    public ConviteInicial aceitar(UUID usuario) {
        return new ConviteInicial(id, nome, email, "ACEITO", expiraEm, usuario, null);
    }
    public ConviteInicial concluir(UUID empresa) {
        return new ConviteInicial(id, nome, email, "CONCLUIDO", expiraEm, usuarioId, empresa);
    }
}
