package com.advocacia_microservice.escritorio.domain.repository;
import com.advocacia_microservice.escritorio.domain.VinculoEscritorio;
import java.util.*;
public interface VinculoRepository {
    void salvar(VinculoEscritorio vinculo);
    List<VinculoEscritorio> listarAtivos(UUID usuarioId);
    Optional<VinculoEscritorio> buscarAtivo(UUID escritorioId, UUID usuarioId);
}
