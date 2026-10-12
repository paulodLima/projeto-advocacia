package com.advocacia_microservice.auth.application;

import com.advocacia_microservice.auth.infrastructure.SessaoCadastro.Pendente;
import com.advocacia_microservice.auth.infrastructure.persistence.entity.*;
import com.advocacia_microservice.auth.infrastructure.persistence.repository.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConcluirCadastroService {
    private final CriarUsuarioUseCase criar;
    private final GoogleIdentityRepository identities;
    private final CadastroAutorizadoService autorizacao;
    public ConcluirCadastroService(CriarUsuarioUseCase criar, GoogleIdentityRepository identities, CadastroAutorizadoService autorizacao) {
        this.criar = criar;
        this.identities = identities;
        this.autorizacao = autorizacao;
    }
    @Transactional
    public Usuario executar(String nome, Pendente pendente) {
        autorizacao.exigirConvite(pendente.email());
        var usuario = criar.executar(nome, pendente.email());
        if (pendente.googleSubject() != null) {
            var identidade = new GoogleIdentityEntity();
            identidade.usuarioId = usuario.id();
            identidade.subject = pendente.googleSubject();
            identities.saveAndFlush(identidade);
        }
        autorizacao.aceitar(usuario);
        autorizacao.exigirConviteAceito(usuario);
        return usuario;
    }
}
