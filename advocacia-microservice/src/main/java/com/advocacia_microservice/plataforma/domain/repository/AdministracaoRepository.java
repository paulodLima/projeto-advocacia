package com.advocacia_microservice.plataforma.domain.repository;
import java.util.UUID;
public interface AdministracaoRepository {
    boolean estaAtivo(UUID usuarioId);
    long contar();
    void conceder(UUID usuarioId);
}
