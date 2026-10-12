package com.advocacia_microservice.plataforma.api.response;
import com.advocacia_microservice.plataforma.domain.ConviteInicial;
import java.time.Instant;
import java.util.UUID;

public record ConviteInicialResponse(UUID id, String nome, String email, String status, Instant expiraEm, UUID empresaId) {
    public static ConviteInicialResponse de(ConviteInicial c) {
        String status = (c.status().equals("PENDENTE") || c.status().equals("ACEITO")) && !c.expiraEm().isAfter(Instant.now()) ? "EXPIRADO" : c.status();
        return new ConviteInicialResponse(c.id(), c.nome(), c.email(), status, c.expiraEm(), c.empresaId());
    }
}
