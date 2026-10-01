package com.advocacia_microservice.auth;

import com.advocacia_microservice.auth.application.CodigoAcessoService;
import com.advocacia_microservice.auth.infrastructure.EmailCodigoSender;
import com.advocacia_microservice.auth.infrastructure.persistence.CodigoAcessoRepository;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.StatusUsuario;
import com.advocacia_microservice.usuario.infrastructure.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthTests {
    @Autowired CodigoAcessoService service;
    @Autowired CriarUsuarioUseCase criar;
    @Autowired CodigoAcessoRepository codigos;
    @Autowired JpaUsuarioRepository usuarios;
    @Autowired WebApplicationContext context;
    @MockitoBean EmailCodigoSender sender;
    MockMvc mvc;

    @BeforeEach
    void preparar() {
        codigos.deleteAll();
        usuarios.deleteAll();
        reset(sender);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String codigoEnviado() {
        var captor = ArgumentCaptor.forClass(String.class);
        verify(sender).enviar(eq("teste@example.com"), captor.capture());
        return captor.getValue();
    }

    @Test
    void autenticaCriaSessaoImpedeReusoESai() throws Exception {
        var usuario = criar.executar("Teste", "teste@example.com");
        var desafio = service.solicitar(usuario.email());
        String codigo = codigoEnviado();
        assertNotEquals(codigo, codigos.findById(usuario.id()).orElseThrow().hash);
        var resultado = mvc.perform(post("/api/auth/validar").with(csrf())
                .contentType("application/json")
                .content("{\"desafioId\":\"" + desafio + "\",\"codigo\":\"" + codigo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.id().toString())).andReturn();
        var session = (MockHttpSession) resultado.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        assertTrue(service.validar(desafio, codigo).isEmpty());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void bloqueiaTentativasEExpiracao() {
        var usuario = criar.executar("Teste", "teste@example.com");
        UUID desafio = service.solicitar(usuario.email());
        String correto = codigoEnviado();
        String errado = correto.equals("000000") ? "111111" : "000000";
        for (int tentativa = 0; tentativa < 5; tentativa++) {
            assertTrue(service.validar(desafio, errado).isEmpty());
        }
        assertTrue(service.validar(desafio, correto).isEmpty());
        var codigo = codigos.findById(usuario.id()).orElseThrow();
        codigo.tentativas = 0;
        codigo.expiraEm = Instant.now().minusSeconds(1);
        codigos.saveAndFlush(codigo);
        assertTrue(service.validar(desafio, correto).isEmpty());
    }

    @Test
    void naoEnviaParaInativoELimitaReenvio() {
        var usuario = criar.executar("Teste", "teste@example.com");
        usuarios.saveAndFlush(UsuarioEntity.de(usuario.atualizar(usuario.nome(), usuario.email(), StatusUsuario.INATIVO)));
        service.solicitar(usuario.email());
        verifyNoInteractions(sender);
        usuarios.saveAndFlush(UsuarioEntity.de(usuario));
        var desafio = service.solicitar(usuario.email());
        service.solicitar(usuario.email());
        String codigo = codigoEnviado(); // Apenas um envio, mesmo após a segunda solicitação.
        assertTrue(service.validar(desafio, codigo).isPresent());
    }

    @Test
    void exigeSessaoECsrfEPermiteObterToken() throws Exception {
        mvc.perform(get("/api/auth/providers")).andExpect(status().isOk()).andExpect(jsonPath("$.google").value(false));
        mvc.perform(get("/api/usuarios/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/codigo").contentType("application/json")
                .content("{\"email\":\"teste@example.com\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void novoEmailValidaSemAcessoEConcluiCadastroComEmailDaSessao() throws Exception {
        mvc.perform(post("/api/auth/cadastro").with(csrf()).contentType("application/json")
            .content("{\"nome\":\"Sem validação\"}")).andExpect(status().isUnauthorized());
        var desafio = service.solicitar("teste@example.com");
        var codigo = codigoEnviado();
        var result = mvc.perform(post("/api/auth/validar").with(csrf()).contentType("application/json")
            .content("{\"desafioId\":\"" + desafio + "\",\"codigo\":\"" + codigo + "\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.cadastroPendente").value(true)).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertEquals(0, usuarios.count());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/" + UUID.randomUUID()).session(session)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/cadastro").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("teste@example.com"));
        mvc.perform(post("/api/auth/cadastro").session(session).with(csrf()).contentType("application/json")
            .content("{\"nome\":\"  Novo usuário  \",\"email\":\"outro@example.com\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("teste@example.com"))
            .andExpect(jsonPath("$.nome").value("Novo usuário"));
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/cadastro").session(session).with(csrf()).contentType("application/json")
            .content("{\"nome\":\"Repetir\"}")).andExpect(status().isUnauthorized());
        assertTrue(service.validar(desafio, codigo).isEmpty());
        assertEquals(1, usuarios.count());
    }

    @Test
    void cadastroExpiradoNaoCriaConta() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("CADASTRO_VALIDADO", new com.advocacia_microservice.auth.infrastructure.SessaoCadastro.Pendente(
            "novo@example.com", null, Instant.now().minusSeconds(1)));
        mvc.perform(post("/api/auth/cadastro").session(session).with(csrf()).contentType("application/json")
            .content("{\"nome\":\"Novo\"}")).andExpect(status().isUnauthorized());
        assertEquals(0, usuarios.count());
    }
}
