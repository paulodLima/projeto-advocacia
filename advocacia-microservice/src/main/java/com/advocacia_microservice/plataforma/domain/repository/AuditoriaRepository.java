package com.advocacia_microservice.plataforma.domain.repository;
import java.util.UUID;
public interface AuditoriaRepository {
    void registrar(UUID autorId, String acao, UUID alvoId);
}
