package com.advocacia_microservice.empresa.application;

import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.usuario.application.*;
import com.advocacia_microservice.usuario.domain.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aplica o vínculo empresarial aos endpoints de usuário já existentes. */
@Service
public class UsuariosDaEmpresaService {
    private final EmpresaService empresas;
    private final EmpresaRepository vinculos;
    private final CriarUsuarioUseCase criar;
    private final BuscarUsuarioUseCase buscar;
    private final AtualizarUsuarioUseCase atualizar;
    private final ExcluirUsuarioUseCase excluir;
    private final com.advocacia_microservice.equipe.infrastructure.persistence.MembroEquipeRepository membros;
    public UsuariosDaEmpresaService(EmpresaService empresas, EmpresaRepository vinculos,
            CriarUsuarioUseCase criar, BuscarUsuarioUseCase buscar,
            AtualizarUsuarioUseCase atualizar, ExcluirUsuarioUseCase excluir, com.advocacia_microservice.equipe.infrastructure.persistence.MembroEquipeRepository membros) {
        this.empresas = empresas; this.vinculos = vinculos; this.criar = criar;
        this.buscar = buscar; this.atualizar = atualizar; this.excluir = excluir;
        this.membros = membros;
    }
    @Transactional
    public Usuario criar(UUID solicitante, String nome, String email) {
        var master = empresas.exigirMaster(solicitante);
        var usuario = criar.executar(nome, email);
        vinculos.vincular(new VinculoEmpresa(usuario.id(), master.empresaId(), PapelEmpresa.MEMBRO));
        var membro = new com.advocacia_microservice.equipe.infrastructure.persistence.MembroEquipeEntity();
        membro.usuarioId = usuario.id(); membros.saveAndFlush(membro);
        return usuario;
    }
    @Transactional(readOnly = true)
    public Usuario buscar(UUID solicitante, UUID alvo) {
        empresas.autorizarUsuario(solicitante, alvo, false);
        return buscar.executar(alvo);
    }
    @Transactional
    public Usuario atualizar(UUID solicitante, UUID alvo, String nome, String email, StatusUsuario status) {
        empresas.autorizarUsuario(solicitante, alvo, true);
        var usuario = atualizar.executar(alvo, nome, email, status);
        membros.findById(alvo).ifPresent(membro -> {
            membro.situacao = status == StatusUsuario.ATIVO ? com.advocacia_microservice.equipe.domain.SituacaoEquipe.ATIVO : com.advocacia_microservice.equipe.domain.SituacaoEquipe.INATIVO;
            membros.saveAndFlush(membro);
        });
        return usuario;
    }
    @Transactional
    public void excluir(UUID solicitante, UUID alvo) {
        empresas.autorizarUsuario(solicitante, alvo, true);
        excluir.executar(alvo);
    }
}
