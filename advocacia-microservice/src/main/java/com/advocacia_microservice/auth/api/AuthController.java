package com.advocacia_microservice.auth.api;
import com.advocacia_microservice.auth.api.request.CadastroRequest;

import com.advocacia_microservice.auth.application.CodigoAcessoService;
import com.advocacia_microservice.auth.application.ConcluirCadastroService;
import com.advocacia_microservice.auth.application.CadastroAutorizadoService;
import com.advocacia_microservice.auth.api.request.*;
import com.advocacia_microservice.auth.api.response.*;
import com.advocacia_microservice.auth.infrastructure.SessaoCadastro;
import com.advocacia_microservice.auth.infrastructure.LoginRateLimiter;
import com.advocacia_microservice.usuario.api.response.UsuarioResponse;
import com.advocacia_microservice.auth.application.ContaAutenticadaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CodigoAcessoService codigos;
    private final ContaAutenticadaService contas;
    private final LoginRateLimiter limite;
    private final ConcluirCadastroService cadastro;
    private final CadastroAutorizadoService autorizacao;


    public AuthController(CodigoAcessoService codigos, ContaAutenticadaService contas, LoginRateLimiter limite, ConcluirCadastroService cadastro, CadastroAutorizadoService autorizacao) {
        this.codigos = codigos;
        this.contas = contas;
        this.limite = limite;
        this.cadastro = cadastro;
        this.autorizacao = autorizacao;
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
                    "Confira seu email para validar o acesso. Se sua conta estiver desativada, fale com o administrador."));
        } catch (MailException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(java.util.Map.of("detail", "Não foi possível enviar o email. Tente novamente mais tarde."));
        }
    }

    @PostMapping("/validar")
    public ResponseEntity<?> validar(@Valid @RequestBody ValidarRequest body,
                                    HttpServletRequest request, HttpServletResponse response) {
        var emailValidado = codigos.validar(body.desafioId(), body.codigo());
        if (emailValidado.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("detail", "Código inválido ou expirado. Solicite um novo código."));
        }
        var conta = contas.buscarPorEmail(emailValidado.get());
        if (conta.isEmpty()) {
            autorizacao.exigirConvite(emailValidado.get());
            SessaoCadastro.iniciar(request, response, emailValidado.get(), null);
            return ResponseEntity.ok(java.util.Map.of("cadastroPendente", true));
        }
        if (conta.get().status() != com.advocacia_microservice.usuario.domain.StatusUsuario.ATIVO) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("detail", "Sua conta está desativada. Fale com o responsável pelo escritório."));
        }
        autorizacao.aceitar(conta.get());
        SessaoCadastro.autenticar(request, response, conta.get());
        return ResponseEntity.ok(UsuarioResponse.de(conta.get()));
    }

    @GetMapping("/cadastro")
    public java.util.Map<String, String> cadastroPendente(HttpServletRequest request) {
        var pendente = SessaoCadastro.obter(request);
        autorizacao.exigirConvite(pendente.email());
        return java.util.Map.of("email", pendente.email());
    }

    @PostMapping("/cadastro")
    public UsuarioResponse cadastrar(@Valid @RequestBody CadastroRequest body,
                                     HttpServletRequest request, HttpServletResponse response) {
        var usuario = cadastro.executar(body.nome(), SessaoCadastro.obter(request));
        SessaoCadastro.autenticar(request, response, usuario);
        return UsuarioResponse.de(usuario);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(java.security.Principal principal) {
        var usuario = contas.buscarAtivo(UUID.fromString(principal.getName()));
        return usuario.<ResponseEntity<?>>map(item -> ResponseEntity.ok(UsuarioResponse.de(item)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
