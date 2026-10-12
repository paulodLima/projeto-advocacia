package com.advocacia_microservice.empresa.application;

import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.shared.exception.*;
import com.advocacia_microservice.usuario.infrastructure.persistence.repository.JpaUsuarioRepository;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaService {
    public record MinhaEmpresa(UUID id, PapelEmpresa papel, Map<String, String> dados, boolean podeCadastrar) {}
    private final EmpresaRepository repository;
    private final JpaUsuarioRepository usuarios;
    private final com.advocacia_microservice.plataforma.application.ConvitesIniciaisService convites;
    public EmpresaService(EmpresaRepository repository, JpaUsuarioRepository usuarios, com.advocacia_microservice.plataforma.application.ConvitesIniciaisService convites) {
        this.repository = repository; this.usuarios = usuarios; this.convites = convites;
    }

    @Transactional(readOnly = true)
    public MinhaEmpresa buscar(UUID usuarioId) {
        var vinculo = repository.vinculo(usuarioId);
        if (vinculo.isEmpty()) return new MinhaEmpresa(null, null, Map.of(),
                usuarios.findById(usuarioId).map(u -> convites.permiteCriarEscritorio(u.paraDominio())).orElse(false));
        var empresa = repository.buscar(vinculo.get().empresaId()).orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada."));
        return new MinhaEmpresa(empresa.id(), vinculo.get().papel(), empresa.dados(), false);
    }

    @Transactional
    public MinhaEmpresa criar(UUID usuarioId, Map<String, String> dados) {
        bloquearUsuario(usuarioId);
        if (repository.vinculo(usuarioId).isPresent()) throw new ConflitoException("Sua conta já está vinculada a uma empresa.");
        var usuario = usuarios.findById(usuarioId).orElseThrow().paraDominio();
        if (!convites.permiteCriarEscritorio(usuario)) throw new AccessDeniedException("Solicite um convite válido ao administrador da plataforma para cadastrar seu escritório.");
        var empresa = new Empresa(UUID.randomUUID(), dados);
        validarCnpj(empresa);
        repository.salvar(empresa);
        convites.autorizarEConcluir(usuario, empresa.id());
        repository.vincular(new VinculoEmpresa(usuarioId, empresa.id(), PapelEmpresa.MASTER));
        return new MinhaEmpresa(empresa.id(), PapelEmpresa.MASTER, empresa.dados(), false);
    }

    @Transactional
    public MinhaEmpresa atualizar(UUID usuarioId, Map<String, String> dados) {
        bloquearUsuario(usuarioId);
        var vinculo = exigirMaster(usuarioId);
        var empresa = new Empresa(vinculo.empresaId(), dados);
        validarCnpj(empresa);
        repository.salvar(empresa);
        return new MinhaEmpresa(empresa.id(), vinculo.papel(), empresa.dados(), false);
    }

    public VinculoEmpresa exigirMaster(UUID usuarioId) {
        var vinculo = repository.vinculo(usuarioId).orElseThrow(() -> new AccessDeniedException("Cadastre sua empresa antes de administrar usuários."));
        if (vinculo.papel() != PapelEmpresa.MASTER) throw new AccessDeniedException("Somente o master pode alterar os dados da empresa ou administrar usuários.");
        if (!estaAtiva(vinculo.empresaId())) throw new AccessDeniedException("Seu escritório está desativado.");
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
    public boolean estaAtiva(UUID empresaId) { return repository.estaAtiva(empresaId); }
    private void validarCnpj(Empresa empresa) {
        if (repository.existeOutroCnpj(empresa.dados().get("cnpj"), empresa.id())) throw new ConflitoException("Já existe uma empresa com esse CNPJ. Solicite um convite ao administrador.");
    }
}
