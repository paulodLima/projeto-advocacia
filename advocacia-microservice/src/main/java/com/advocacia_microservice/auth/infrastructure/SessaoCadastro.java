package com.advocacia_microservice.auth.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import com.advocacia_microservice.usuario.domain.Usuario;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

public final class SessaoCadastro {
    private static final String CHAVE = "CADASTRO_VALIDADO";
    public record Pendente(String email, String googleSubject, Instant expiraEm) implements Serializable {}
    private SessaoCadastro() {}
    public static void iniciar(HttpServletRequest request, HttpServletResponse response, String email, String subject) {
        request.getSession();
        request.changeSessionId();
        var context = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        request.getSession().setAttribute(CHAVE, new Pendente(email, subject, Instant.now().plusSeconds(900)));
    }
    public static Pendente obter(HttpServletRequest request) {
        var session = request.getSession(false);
        var value = session == null ? null : session.getAttribute(CHAVE);
        if (!(value instanceof Pendente pendente) || !pendente.expiraEm().isAfter(Instant.now())) {
            if (session != null) session.removeAttribute(CHAVE);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valide seu email novamente para criar a conta.");
        }
        return pendente;
    }
    public static void autenticar(HttpServletRequest request, HttpServletResponse response, Usuario usuario) {
        request.getSession();
        request.changeSessionId();
        request.getSession().removeAttribute(CHAVE);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            usuario.id().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
    }
}
