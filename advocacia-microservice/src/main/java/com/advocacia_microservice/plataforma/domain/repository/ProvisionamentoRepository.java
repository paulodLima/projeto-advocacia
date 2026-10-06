package com.advocacia_microservice.plataforma.domain.repository;
public interface ProvisionamentoRepository {
    boolean bloquearEConsultarConclusao();
    void concluir();
}
