package com.advocacia_microservice.plataforma.application;

import com.advocacia_microservice.plataforma.domain.GestaoEmpresa;
import com.advocacia_microservice.plataforma.domain.repository.GestaoEmpresaRepository;
import com.advocacia_microservice.plataforma.application.command.AlterarContrato;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestaoEmpresasService {
    private final GestaoEmpresaRepository repository;
    public GestaoEmpresasService(GestaoEmpresaRepository repository) { this.repository = repository; }
    @Transactional(readOnly=true)
    public List<GestaoEmpresa> listar(String busca, String situacao, int pagina) {
        if (pagina < 0 || busca.length() > 150 || !Set.of("", "ATIVA", "SUSPENSA", "ENCERRADA").contains(situacao))
            throw new IllegalArgumentException("Filtro inválido.");
        return repository.listar(busca.strip(), situacao, pagina);
    }
    @Transactional(readOnly=true)
    public GestaoEmpresa buscar(UUID id) { return repository.buscar(id); }
    @Transactional
    public GestaoEmpresa alterar(UUID id, AlterarContrato c, UUID autor) {
        if (!Set.of("ATIVA", "SUSPENSA", "ENCERRADA").contains(c.situacao()) ||
                !Set.of("NAO_INFORMADO", "REGULAR", "INADIMPLENTE").contains(c.financeiro()) ||
                c.motivo() == null || c.motivo().isBlank() || c.motivo().length() > 1000 || c.modulos() == null || c.versao() < 0)
            throw new IllegalArgumentException("Informe situação, financeiro, módulos e motivo válido.");
        var atual = repository.buscar(id);
        var catalogo = atual.modulos().stream().map(GestaoEmpresa.Modulo::codigo).collect(java.util.stream.Collectors.toSet());
        if (!catalogo.containsAll(c.modulos()) || !c.modulos().containsAll(Set.of("inicio","config")))
            throw new IllegalArgumentException("Módulo desconhecido ou ausência de Início/Configurações, necessários para acessar a conta.");
        repository.alterar(id, c.versao(), c.situacao(), c.financeiro(), c.inicio(), c.modulos(), autor, c.motivo().strip());
        return repository.buscar(id);
    }
    @Transactional(readOnly=true)
    public Set<String> modulosPermitidos(UUID empresaId) { return repository.modulosPermitidos(empresaId); }
}
