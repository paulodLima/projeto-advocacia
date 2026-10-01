package com.advocacia_microservice.auth.application;

import com.advocacia_microservice.auth.infrastructure.SessaoCadastro.Pendente;
import com.advocacia_microservice.auth.infrastructure.persistence.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConcluirCadastroService {
    private final CriarUsuarioUseCase criar;
    private final GoogleIdentityRepository identities;
    public ConcluirCadastroService(CriarUsuarioUseCase criar, GoogleIdentityRepository identities) {
        this.criar = criar;
        this.identities = identities;
    }
    @Transactional
    public Usuario executar(String nome, Pendente pendente) {
        var usuario = criar.executar(nome, pendente.email());
        if (pendente.googleSubject() != null) {
            var identidade = new GoogleIdentityEntity();
            identidade.usuarioId = usuario.id();
            identidade.subject = pendente.googleSubject();
            identities.saveAndFlush(identidade);
        }
        return usuario;
    }
}
