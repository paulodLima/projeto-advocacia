package com.advocacia_microservice.empresa.application;

import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.shared.exception.*;
import com.advocacia_microservice.usuario.infrastructure.persistence.JpaUsuarioRepository;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaService {
    public record MinhaEmpresa(UUID id, PapelEmpresa papel, Map<String, String> dados) {}
    private final EmpresaRepository repository;
    private final JpaUsuarioRepository usuarios;
    public EmpresaService(EmpresaRepository repository, JpaUsuarioRepository usuarios) {
        this.repository = repository; this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public MinhaEmpresa buscar(UUID usuarioId) {
        var vinculo = repository.vinculo(usuarioId);
        if (vinculo.isEmpty()) return new MinhaEmpresa(null, null, Map.of());
        var empresa = repository.buscar(vinculo.get().empresaId()).orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada."));
        return new MinhaEmpresa(empresa.id(), vinculo.get().papel(), empresa.dados());
    }

    @Transactional
    public MinhaEmpresa criar(UUID usuarioId, Map<String, String> dados) {
        bloquearUsuario(usuarioId);
        if (repository.vinculo(usuarioId).isPresent()) throw new ConflitoException("Sua conta já está vinculada a uma empresa.");
        var empresa = new Empresa(UUID.randomUUID(), dados);
        validarCnpj(empresa);
        repository.salvar(empresa);
        repository.vincular(new VinculoEmpresa(usuarioId, empresa.id(), PapelEmpresa.MASTER));
        return new MinhaEmpresa(empresa.id(), PapelEmpresa.MASTER, empresa.dados());
    }

    @Transactional
    public MinhaEmpresa atualizar(UUID usuarioId, Map<String, String> dados) {
        bloquearUsuario(usuarioId);
        var vinculo = exigirMaster(usuarioId);
        var empresa = new Empresa(vinculo.empresaId(), dados);
        validarCnpj(empresa);
        repository.salvar(empresa);
        return new MinhaEmpresa(empresa.id(), vinculo.papel(), empresa.dados());
    }

    public VinculoEmpresa exigirMaster(UUID usuarioId) {
        var vinculo = repository.vinculo(usuarioId).orElseThrow(() -> new AccessDeniedException("Cadastre sua empresa antes de administrar usuários."));
        if (vinculo.papel() != PapelEmpresa.MASTER) throw new AccessDeniedException("Somente o master pode alterar os dados da empresa ou administrar usuários.");
        return vinculo;
    }

    public void autorizarUsuario(UUID solicitante, UUID alvo, boolean escrita) {
        if (!escrita && solicitante.equals(alvo)) return;
        var master = exigirMaster(solicitante);
        var destino = repository.vinculo(alvo).orElseThrow(() -> new AccessDeniedException("Usuário fora da sua empresa."));
        if (!master.empresaId().equals(destino.empresaId())) throw new AccessDeniedException("Usuário fora da sua empresa.");
        if (escrita && destino.papel() == PapelEmpresa.MASTER) throw new AccessDeniedException("O usuário master não pode ser alterado ou excluído por esse endpoint.");
    }

    private void bloquearUsuario(UUID id) {
        usuarios.buscarParaAtualizar(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }
    private void validarCnpj(Empresa empresa) {
        if (repository.existeOutroCnpj(empresa.dados().get("cnpj"), empresa.id())) throw new ConflitoException("Já existe uma empresa com esse CNPJ. Solicite um convite ao administrador.");
    }
}
