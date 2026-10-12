package com.advocacia_microservice.plataforma.domain.repository;

import com.advocacia_microservice.plataforma.domain.ConviteInicial;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConviteInicialRepository {
    List<ConviteInicial> listar();
    Optional<ConviteInicial> buscar(UUID id);
    Optional<ConviteInicial> buscarPorEmail(String email);
    Optional<ConviteInicial> bloquearPorEmail(String email);
    ConviteInicial salvar(ConviteInicial convite);
}
