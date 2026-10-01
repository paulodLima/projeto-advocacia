package com.advocacia_microservice.auth;

import com.advocacia_microservice.auth.application.GoogleLoginService;
import com.advocacia_microservice.auth.infrastructure.persistence.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.StatusUsuario;
import com.advocacia_microservice.usuario.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.auth.google.client-id=test-client",
        "app.auth.google.client-secret=test-secret"
})
@ActiveProfiles("test")
class GoogleLoginTests {
    @Autowired GoogleLoginService login;
    @Autowired CriarUsuarioUseCase criar;
    @Autowired JpaUsuarioRepository usuarios;
    @Autowired GoogleIdentityRepository identities;
    @Autowired CodigoAcessoRepository codigos;
    @Autowired WebApplicationContext context;
    @Autowired com.advocacia_microservice.auth.infrastructure.GoogleLoginSuccessHandler success;
    MockMvc mvc;

    @BeforeEach
    void preparar() {
        identities.deleteAll();
        codigos.deleteAll();
        usuarios.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private OidcUser google(String subject, String email, boolean verified) {
        var google = mock(OidcUser.class);
        when(google.getSubject()).thenReturn(subject);
        when(google.getEmail()).thenReturn(email);
        when(google.getEmailVerified()).thenReturn(verified);
        return google;
    }

    @Test
    void vinculaGmailCadastradoEUsaIdentificadorEstavelDepois() {
        var usuario = criar.executar("Teste", "teste@gmail.com");
        assertEquals(usuario.id(), login.autenticar(google("google-subject", "TESTE@gmail.com", true)).id());
        assertEquals("google-subject", identities.findById(usuario.id()).orElseThrow().subject);
        assertEquals(usuario.id(), login.autenticar(google("google-subject", "outro@gmail.com", true)).id());
        assertEquals(1, identities.count());
    }

    @Test
    void emailNovoRequerCadastroEEmailNaoVerificadoEhNegado() {
        assertNull(login.autenticar(google("unknown", "ausente@gmail.com", true)));
        criar.executar("Teste", "teste@gmail.com");
        assertThrows(OAuth2AuthenticationException.class,
                () -> login.autenticar(google("unverified", "teste@gmail.com", false)));
        assertEquals(0, identities.count());
    }

    @Test
    void rejeitaInativoEOutraContaGoogleParaOMesmoUsuario() {
        var usuario = criar.executar("Teste", "teste@gmail.com");
        login.autenticar(google("original", "teste@gmail.com", true));
        assertThrows(OAuth2AuthenticationException.class,
                () -> login.autenticar(google("different", "teste@gmail.com", true)));
        usuarios.saveAndFlush(UsuarioEntity.de(usuario.atualizar(usuario.nome(), usuario.email(), StatusUsuario.INATIVO)));
        assertThrows(OAuth2AuthenticationException.class,
                () -> login.autenticar(google("original", "teste@gmail.com", true)));
    }

    @Test
    void exigeAutoridadeDoGoogleNoPrimeiroVinculo() {
        var usuario = criar.executar("Teste", "teste@empresa.test");
        assertThrows(OAuth2AuthenticationException.class,
                () -> login.autenticar(google("external", usuario.email(), true)));
        var workspace = google("workspace", usuario.email(), true);
        when(workspace.getClaimAsString("hd")).thenReturn("empresa.test");
        assertEquals(usuario.id(), login.autenticar(workspace).id());
    }

    @Test
    void publicaDisponibilidadeEIniciaOauthSemChamadasExternas() throws Exception {
        mvc.perform(get("/api/auth/providers")).andExpect(status().isOk()).andExpect(jsonPath("$.google").value(true));
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://accounts.google.com/")));
        mvc.perform(get("/login/oauth2/code/google").param("code", "sem-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4200/login?erro=google_falhou"));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void callbackCriaSessaoLocalEContaNegadaNaoRecebeSessao() throws Exception {
        var usuario = criar.executar("Teste", "teste@gmail.com");
        var principal = google("session-subject", usuario.email(), true);
        var authentication = new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(
                principal, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")), "google");
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        request.getSession();
        success.onAuthenticationSuccess(request, response, authentication);
        assertEquals("http://localhost:4200/inicio", response.getRedirectedUrl());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        var session = (org.springframework.mock.web.MockHttpSession) request.getSession(false);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.id().toString()));

        var deniedRequest = new org.springframework.mock.web.MockHttpServletRequest();
        var deniedSession = deniedRequest.getSession();
        var deniedResponse = new org.springframework.mock.web.MockHttpServletResponse();
        var denied = new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(
                google("denied-subject", "ausente@gmail.com", false), authentication.getAuthorities(), "google");
        success.onAuthenticationSuccess(deniedRequest, deniedResponse, denied);
        assertEquals("http://localhost:4200/login?erro=google_acesso_negado", deniedResponse.getRedirectedUrl());
        assertTrue(((org.springframework.mock.web.MockHttpSession) deniedSession).isInvalid());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void googleNovoVaiParaCadastroEVinculaContaAoConcluir() throws Exception {
        var principal = google("new-subject", "novo@gmail.com", true);
        var authentication = new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(
            principal, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")), "google");
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        success.onAuthenticationSuccess(request, response, authentication);
        assertEquals("http://localhost:4200/cadastro", response.getRedirectedUrl());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        var session = (org.springframework.mock.web.MockHttpSession) request.getSession(false);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/cadastro").session(session).with(csrf()).contentType("application/json")
            .content("{\"nome\":\"Novo Google\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("novo@gmail.com"));
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        assertEquals(1, identities.count());
        assertNotNull(login.autenticar(principal));
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
