package com.advocacia_microservice.escritorio.application;

import com.advocacia_microservice.escritorio.domain.*;
import com.advocacia_microservice.escritorio.domain.repository.EmpresaRepository;
import com.advocacia_microservice.escritorio.domain.repository.VinculoRepository;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EscritorioService {
    private final EmpresaRepository empresas;
    private final VinculoRepository membros;
    public EscritorioService(EmpresaRepository empresas, VinculoRepository membros) {
        this.empresas = empresas; this.membros = membros;
    }
    @Transactional
    public AcessoEscritorio criar(UUID usuarioId, String nome) {
        UUID id = UUID.randomUUID();
        if (nome.trim().isEmpty()) throw new IllegalArgumentException("Informe o nome do escritório.");
        var e = empresas.salvar(new Empresa(id, nome.trim(), "empresa-" + id, true,
                null, null, null, null, null, null, null, null, null));
        membros.salvar(new VinculoEscritorio(UUID.randomUUID(), id, usuarioId, "PROPRIETARIO", true));
        return new AcessoEscritorio(e.id(), e.nome(), "PROPRIETARIO");
    }
    @Transactional(readOnly = true)
    public List<AcessoEscritorio> listar(UUID usuarioId) {
        var vinculos = membros.listarAtivos(usuarioId);
        var porId = new HashMap<UUID, Empresa>();
        empresas.buscarPorIds(vinculos.stream().map(VinculoEscritorio::escritorioId).toList()).forEach(e -> porId.put(e.id(), e));
        return vinculos.stream().filter(m -> porId.containsKey(m.escritorioId()) && porId.get(m.escritorioId()).ativo())
                .map(m -> new AcessoEscritorio(m.escritorioId(), porId.get(m.escritorioId()).nome(), m.perfil()))
                .sorted(Comparator.comparing(AcessoEscritorio::nome).thenComparing(AcessoEscritorio::id)).toList();
    }
    @Transactional(readOnly = true)
    public AcessoEscritorio validar(UUID usuarioId, UUID escritorioId) {
        var membro = membros.buscarAtivo(escritorioId, usuarioId).orElseThrow(() -> new AccessDeniedException("Sem acesso ao escritório."));
        var e = empresas.buscarPorId(escritorioId).orElseThrow(() -> new AccessDeniedException("Sem acesso ao escritório."));
        if (!e.ativo()) throw new AccessDeniedException("Empresa inativa.");
        return new AcessoEscritorio(e.id(), e.nome(), membro.perfil());
    }
}
