package com.advocacia_microservice.plataforma.domain.repository;

public interface NotificacaoConviteInicial {
    void enviar(String email, String nome);
}
