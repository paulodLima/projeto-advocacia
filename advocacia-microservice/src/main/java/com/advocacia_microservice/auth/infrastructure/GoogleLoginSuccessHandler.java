package com.advocacia_microservice.auth.infrastructure;

import com.advocacia_microservice.auth.application.GoogleLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final GoogleLoginService login;
    private final String publicUrl;

    public GoogleLoginSuccessHandler(GoogleLoginService login,
                                    @Value("${app.auth.public-url}") String publicUrl) {
        this.login = login;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            if (!(authentication.getPrincipal() instanceof OidcUser google)) {
                throw new OAuth2AuthenticationException("invalid_user");
            }
            var usuario = login.autenticar(google);
            if (usuario == null) {
                SessaoCadastro.iniciar(request, response, google.getEmail().toLowerCase(java.util.Locale.ROOT), google.getSubject());
                response.sendRedirect(publicUrl + "/cadastro");
                return;
            }
            SessaoCadastro.autenticar(request, response, usuario);
            response.sendRedirect(publicUrl + "/inicio");
        } catch (OAuth2AuthenticationException | DataIntegrityViolationException exception) {
            SecurityContextHolder.clearContext();
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            response.sendRedirect(publicUrl + "/login?erro=google_acesso_negado");
        }
    }
}
