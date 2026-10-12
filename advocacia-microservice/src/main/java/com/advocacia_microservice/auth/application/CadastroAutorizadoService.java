package com.advocacia_microservice.auth.application;
import com.advocacia_microservice.equipe.application.EquipeService;
import com.advocacia_microservice.plataforma.application.ConvitesIniciaisService;
import com.advocacia_microservice.usuario.domain.Usuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastroAutorizadoService {
    private final ConvitesIniciaisService iniciais;
    private final EquipeService equipe;
    public CadastroAutorizadoService(ConvitesIniciaisService iniciais, EquipeService equipe) { this.iniciais = iniciais; this.equipe = equipe; }
    @Transactional(readOnly=true)
    public boolean permitido(String email) { return iniciais.permiteCadastro(email) || equipe.permiteCadastro(email); }
    public void exigirConvite(String email) {
        if (!permitido(email)) throw new AccessDeniedException("Acesso não liberado. Solicite um convite ao responsável pelo escritório ou ao administrador da plataforma.");
    }
    @Transactional
    public void aceitar(Usuario usuario) { iniciais.aceitar(usuario); equipe.aceitarConvite(usuario); }
    public void exigirConviteAceito(Usuario usuario) {
        if (!iniciais.permiteCriarEscritorio(usuario) && equipe.acesso(usuario.id()).empresaId() == null) {
            throw new AccessDeniedException("O convite expirou ou foi cancelado. Solicite um novo convite.");
        }
    }
}
