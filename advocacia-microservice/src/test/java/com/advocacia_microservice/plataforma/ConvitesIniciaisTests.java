package com.advocacia_microservice.plataforma;

import com.advocacia_microservice.auth.application.CodigoAcessoService;
import com.advocacia_microservice.auth.application.GoogleLoginService;
import com.advocacia_microservice.auth.infrastructure.EmailCodigoSender;
import com.advocacia_microservice.equipe.infrastructure.EmailConviteSender;
import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.plataforma.application.ConvitesIniciaisService;
import com.advocacia_microservice.plataforma.domain.ConviteInicial;
import com.advocacia_microservice.plataforma.domain.repository.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.Usuario;
import com.advocacia_microservice.usuario.domain.UsuarioRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class ConvitesIniciaisTests {
    @Autowired CriarUsuarioUseCase criar;
    @Autowired UsuarioRepository usuarios;
    @Autowired AdministracaoRepository administradores;
    @Autowired ConviteInicialRepository repository;
    @Autowired ConvitesIniciaisService convites;
    @Autowired CodigoAcessoService codigos;
    @Autowired EmpresaService empresas;
    @Autowired GoogleLoginService google;
    @Autowired WebApplicationContext context;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockitoBean NotificacaoConviteInicial emailInicial;
    @MockitoBean EmailCodigoSender emailCodigo;
    @MockitoBean EmailConviteSender emailEquipe;
    MockMvc mvc;
    Usuario admin;
    String destino;
    static final String EMPRESA = "{\"dados\":{\"razao_social\":\"Escritório Teste\",\"cnpj\":\"11222333000181\",\"endereco\":\"Rua A\",\"cidade\":\"Brasília\"}}";
    @BeforeEach void preparar() {
        admin = criar.executar("Administrador", UUID.randomUUID() + "@example.test");
        administradores.conceder(admin.id()); destino = UUID.randomUUID() + "@example.test";
        reset(emailInicial, emailCodigo, emailEquipe);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    MockHttpSession validar(String email) throws Exception {
        reset(emailCodigo); var desafio = codigos.solicitar(email);
        var codigo = ArgumentCaptor.forClass(String.class); verify(emailCodigo).enviar(eq(email), codigo.capture());
        var r = mvc.perform(post("/api/auth/validar").with(csrf()).contentType("application/json")
                .content("{\"desafioId\":\"" + desafio + "\",\"codigo\":\"" + codigo.getValue() + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }
    Usuario concluir(MockHttpSession sessao, String email) throws Exception {
        mvc.perform(post("/api/auth/cadastro").session(sessao).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"Convidado\",\"email\":\"injetado@example.test\",\"perfil\":\"ADMINISTRADOR_PLATAFORMA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
        return usuarios.buscarPorEmail(email).orElseThrow();
    }
    @Test void administradorConvidaResponsavelCadastraEscritorioEFuncionarioConfirmaConvite() throws Exception {
        mvc.perform(post("/api/plataforma/admin/convites").with(user(admin.id().toString())).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"Responsável\",\"email\":\"" + destino + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDENTE"));
        verify(emailInicial).enviar(destino, "Responsável"); assertTrue(usuarios.buscarPorEmail(destino).isEmpty());
        var sessao = validar(destino);
        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/cadastro").session(sessao)).andExpect(jsonPath("$.email").value(destino));
        var responsavel = concluir(sessao, destino);
        assertFalse(administradores.estaAtivo(responsavel.id()));
        mvc.perform(get("/api/empresas/minha").session(sessao)).andExpect(jsonPath("$.podeCadastrar").value(true));
        mvc.perform(post("/api/empresas").session(sessao).with(csrf()).contentType("application/json").content(EMPRESA))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.papel").value("MASTER"));
        var empresa = empresas.buscar(responsavel.id()).id();
        assertEquals("CONCLUIDO", repository.buscarPorEmail(destino).orElseThrow().status());
        mvc.perform(get("/api/plataforma/admin/convites").session(sessao)).andExpect(status().isForbidden());
        mvc.perform(post("/api/empresas").session(sessao).with(csrf()).contentType("application/json").content(EMPRESA))
                .andExpect(status().isConflict());
        var funcionarioEmail = UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/equipe/convites").session(sessao).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"Funcionário\",\"email\":\"" + funcionarioEmail + "\",\"configuracao\":{\"nomeExibicao\":\"\",\"telefone\":\"\",\"perfil\":\"ASSISTENTE\",\"situacao\":\"ATIVO\",\"restrito\":false,\"abas\":[],\"enviaDocumento\":false}}"))
                .andExpect(status().isCreated());
        var sessaoFuncionario = validar(funcionarioEmail); var funcionario = concluir(sessaoFuncionario, funcionarioEmail);
        assertEquals(empresa, empresas.buscar(funcionario.id()).id());
        mvc.perform(get("/api/equipe/me").session(sessaoFuncionario)).andExpect(jsonPath("$.papel").value("MEMBRO"));
        mvc.perform(post("/api/equipe/convites").session(sessaoFuncionario).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"Outro\",\"email\":\"outro@example.test\",\"configuracao\":{\"nomeExibicao\":\"\",\"telefone\":\"\",\"perfil\":\"ASSISTENTE\",\"situacao\":\"ATIVO\",\"restrito\":false,\"abas\":[],\"enviaDocumento\":false}}"))
                .andExpect(status().isForbidden());
    }
    @Test void emailSemConviteNaoRecebeCodigoNemPodeCriarEscritorio() throws Exception {
        codigos.solicitar(destino); verifyNoInteractions(emailCodigo);
        var conta = criar.executar("Sem convite", destino);
        mvc.perform(post("/api/empresas").with(user(conta.id().toString())).with(csrf()).contentType("application/json").content(EMPRESA))
                .andExpect(status().isForbidden());
        assertNull(empresas.buscar(conta.id()).id());
        mvc.perform(post("/api/plataforma/admin/convites").with(user(conta.id().toString())).with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }
    @Test void conviteCanceladoAposValidacaoNaoPermiteConcluirCadastro() throws Exception {
        var c = convites.convidar(admin.id(), "Pessoa", destino); var sessao = validar(destino);
        convites.cancelar(admin.id(), c.id());
        mvc.perform(post("/api/auth/cadastro").session(sessao).with(csrf()).contentType("application/json").content("{\"nome\":\"Pessoa\"}"))
                .andExpect(status().isForbidden());
        assertTrue(usuarios.buscarPorEmail(destino).isEmpty());
    }
    @Test void cancelarAposCadastroPessoalBloqueiaCriacaoDoEscritorio() throws Exception {
        var c = convites.convidar(admin.id(), "Pessoa", destino); var sessao = validar(destino); var pessoa = concluir(sessao, destino);
        convites.cancelar(admin.id(), c.id());
        mvc.perform(post("/api/empresas").session(sessao).with(csrf()).contentType("application/json").content(EMPRESA)).andExpect(status().isForbidden());
        assertNull(empresas.buscar(pessoa.id()).id());
    }
    @Test void expiracaoReenvioEDuplicidade() throws Exception {
        var c = convites.convidar(admin.id(), "Pessoa", destino);
        assertThrows(com.advocacia_microservice.shared.exception.ConflitoException.class, () -> convites.convidar(admin.id(), "Pessoa", destino));
        repository.salvar(new ConviteInicial(c.id(), c.nome(), c.email(), c.status(), Instant.now().minusSeconds(1), null, null));
        codigos.solicitar(destino); verifyNoInteractions(emailCodigo);
        mvc.perform(get("/api/plataforma/admin/convites").with(user(admin.id().toString())))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("EXPIRADO")));
        convites.reenviar(admin.id(), c.id()); assertTrue(convites.permiteCadastro(destino));
    }
    @Test void googleVerificadoSoIniciaCadastroParaEmailConvidado() {
        var usuario = mock(OidcUser.class);
        var gmail = UUID.randomUUID() + "@gmail.com";
        when(usuario.getSubject()).thenReturn(UUID.randomUUID().toString()); when(usuario.getEmail()).thenReturn(gmail); when(usuario.getEmailVerified()).thenReturn(true);
        assertThrows(OAuth2AuthenticationException.class, () -> google.autenticar(usuario));
        convites.convidar(admin.id(), "Google", gmail);
        when(usuario.getEmailVerified()).thenReturn(false); assertThrows(OAuth2AuthenticationException.class, () -> google.autenticar(usuario));
        when(usuario.getEmailVerified()).thenReturn(true); assertNull(google.autenticar(usuario));
        assertTrue(usuarios.buscarPorEmail(gmail).isEmpty());
    }
    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void falhaDeEmailImpedeGravarConvite() {
        try {
            doThrow(new MailSendException("SMTP indisponível")).when(emailInicial).enviar(destino, "Pessoa");
            assertThrows(MailSendException.class, () -> convites.convidar(admin.id(), "Pessoa", destino));
            assertTrue(repository.buscarPorEmail(destino).isEmpty());
        } finally {
            jdbc.update("DELETE FROM administrador_plataforma WHERE usuario_id=?", admin.id());
            usuarios.excluir(admin);
        }
    }
}
