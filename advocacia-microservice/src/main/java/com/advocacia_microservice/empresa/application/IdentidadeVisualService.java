package com.advocacia_microservice.empresa.application;

import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.empresa.infrastructure.persistence.*;
import com.advocacia_microservice.shared.exception.RecursoNaoEncontradoException;
import com.advocacia_microservice.usuario.infrastructure.persistence.JpaUsuarioRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IdentidadeVisualService {
    public record Resposta(UUID empresaId, String nome, IdentidadeVisual identidade) {}
    private final EmpresaService empresas;
    private final EmpresaRepository empresaRepository;
    private final JpaIdentidadeVisualRepository repository;
    private final JpaUsuarioRepository usuarios;
    public IdentidadeVisualService(EmpresaService empresas, EmpresaRepository empresaRepository, JpaIdentidadeVisualRepository repository, JpaUsuarioRepository usuarios) {
        this.empresas = empresas; this.empresaRepository = empresaRepository; this.repository = repository; this.usuarios = usuarios;
    }
    public Resposta buscar(UUID usuario) {
        var empresa = empresas.buscar(usuario);
        if (empresa.id() == null) throw new RecursoNaoEncontradoException("Cadastre a empresa antes de configurar a identidade visual.");
        return publica(empresa.id());
    }
    public Resposta publica(UUID empresaId) {
        var empresa = empresaRepository.buscar(empresaId).orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada."));
        var nome = empresa.dados().get("nome_fantasia");
        if (nome == null || nome.isBlank()) nome = empresa.dados().get("razao_social");
        return new Resposta(empresaId, nome, repository.findById(empresaId).map(IdentidadeVisualEntity::paraDominio).orElseGet(IdentidadeVisual::padrao));
    }
    @Transactional
    public Resposta salvar(UUID usuario, IdentidadeVisual identidade) {
        usuarios.buscarParaAtualizar(usuario).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        var vinculo = empresas.exigirMaster(usuario);
        repository.saveAndFlush(IdentidadeVisualEntity.de(vinculo.empresaId(), identidade.normalizarImagens()));
        return publica(vinculo.empresaId());
    }
}
