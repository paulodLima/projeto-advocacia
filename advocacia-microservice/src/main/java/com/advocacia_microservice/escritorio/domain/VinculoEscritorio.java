package com.advocacia_microservice.escritorio.domain;
import java.util.UUID;
public record VinculoEscritorio(UUID id, UUID escritorioId, UUID usuarioId, String perfil, boolean ativo) {}
