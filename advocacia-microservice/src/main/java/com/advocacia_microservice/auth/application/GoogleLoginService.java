package com.advocacia_microservice.auth.application;

import com.advocacia_microservice.auth.infrastructure.persistence.*;
import com.advocacia_microservice.usuario.domain.StatusUsuario;
import com.advocacia_microservice.usuario.domain.Usuario;
import com.advocacia_microservice.usuario.infrastructure.persistence.JpaUsuarioRepository;
import java.util.Locale;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleLoginService {
    private final JpaUsuarioRepository usuarios;
    private final GoogleIdentityRepository identities;

    public GoogleLoginService(JpaUsuarioRepository usuarios, GoogleIdentityRepository identities) {
        this.usuarios = usuarios;
        this.identities = identities;
    }

    @Transactional
    public Usuario autenticar(OidcUser google) {
        if (!Boolean.TRUE.equals(google.getEmailVerified()) || google.getSubject() == null) {
            throw negado();
        }
        var vinculo = identities.findBySubject(google.getSubject()).orElse(null);
        if (vinculo != null) {
            return usuarios.findById(vinculo.usuarioId).map(item -> item.paraDominio())
                    .filter(item -> item.status() == StatusUsuario.ATIVO).orElseThrow(this::negado);
        }
        String email = google.getEmail();
        if (email == null) throw negado();
        email = email.toLowerCase(Locale.ROOT);
        // Para o primeiro vínculo, Google precisa ser autoridade sobre o email.
        // Contas externas sem Gmail/Workspace usam o fluxo de código de email.
        String hostedDomain = google.getClaimAsString("hd");
        boolean workspace = hostedDomain != null && !hostedDomain.isBlank()
                && email.endsWith("@" + hostedDomain.toLowerCase(Locale.ROOT));
        if (!email.endsWith("@gmail.com") && !workspace) throw negado();
        var usuario = usuarios.findByEmail(email).map(item -> item.paraDominio()).orElse(null);
        if (usuario == null) return null; // Email verificado: completar cadastro antes de autenticar.
        if (usuario.status() != StatusUsuario.ATIVO) throw negado();
        var existente = identities.findById(usuario.id()).orElse(null);
        if (existente != null && !existente.subject.equals(google.getSubject())) throw negado();
        var identidade = new GoogleIdentityEntity();
        identidade.usuarioId = usuario.id();
        identidade.subject = google.getSubject();
        identities.saveAndFlush(identidade);
        return usuario;
    }

    private OAuth2AuthenticationException negado() {
        return new OAuth2AuthenticationException(new OAuth2Error("access_denied"),
                "Conta Google não autorizada.");
    }
}
