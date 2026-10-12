package com.advocacia_microservice.plataforma.domain.repository;

import com.advocacia_microservice.plataforma.domain.GestaoEmpresa;
import java.time.LocalDate;
import java.util.*;

public interface GestaoEmpresaRepository {
    List<GestaoEmpresa> listar(String busca, String situacao, int pagina);
    GestaoEmpresa buscar(UUID id);
    void alterar(UUID id, long versao, String situacao, String financeiro, LocalDate inicio, Set<String> modulos,
            UUID autor, String motivo);
    Set<String> modulosPermitidos(UUID empresaId);
}
