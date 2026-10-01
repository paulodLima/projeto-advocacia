package com.advocacia_microservice.auth.api;

import com.advocacia_microservice.auth.application.CodigoAcessoService;
import com.advocacia_microservice.auth.infrastructure.LoginRateLimiter;
import com.advocacia_microservice.usuario.api.response.UsuarioResponse;
import com.advocacia_microservice.usuario.domain.StatusUsuario;
import com.advocacia_microservice.usuario.infrastructure.persistence.JpaUsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record SolicitarRequest(@NotBlank @Email @Size(max = 254) String email) {}
    public record ValidarRequest(@NotNull UUID desafioId, @NotBlank @Pattern(regexp = "[0-9]{6}") String codigo) {}
    public record DesafioResponse(UUID desafioId, String mensagem) {}
    public record CsrfResponse(String token, String headerName) {}

    private final CodigoAcessoService codigos;
    private final JpaUsuarioRepository usuarios;
    private final LoginRateLimiter limite;

    public AuthController(CodigoAcessoService codigos, JpaUsuarioRepository usuarios, LoginRateLimiter limite) {
        this.codigos = codigos;
        this.usuarios = usuarios;
        this.limite = limite;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    @PostMapping("/codigo")
    public ResponseEntity<?> solicitar(@Valid @RequestBody SolicitarRequest body, HttpServletRequest request) {
        if (!limite.permitir(request.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(java.util.Map.of("detail", "Muitas solicitações. Tente novamente mais tarde."));
        }
        try {
            return ResponseEntity.ok(new DesafioResponse(codigos.solicitar(body.email()),
                    "Se este email estiver cadastrado e ativo, você receberá um código de acesso."));
        } catch (MailException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(java.util.Map.of("detail", "Não foi possível enviar o email. Tente novamente mais tarde."));
        }
    }

    @PostMapping("/validar")
    public ResponseEntity<?> validar(@Valid @RequestBody ValidarRequest body,
                                    HttpServletRequest request, HttpServletResponse response) {
        var usuario = codigos.validar(body.desafioId(), body.codigo());
        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("detail", "Código inválido ou expirado. Solicite um novo código."));
        }
        request.getSession();
        request.changeSessionId();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                usuario.get().id().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        return ResponseEntity.ok(UsuarioResponse.de(usuario.get()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(java.security.Principal principal) {
        var usuario = usuarios.findById(UUID.fromString(principal.getName()))
                .map(entity -> entity.paraDominio())
                .filter(item -> item.status() == StatusUsuario.ATIVO);
        return usuario.<ResponseEntity<?>>map(item -> ResponseEntity.ok(UsuarioResponse.de(item)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
