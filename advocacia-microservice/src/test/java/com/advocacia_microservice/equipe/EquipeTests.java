package com.advocacia_microservice.equipe;

import com.advocacia_microservice.auth.application.*;
import com.advocacia_microservice.auth.infrastructure.EmailCodigoSender;
import com.advocacia_microservice.equipe.api.ConfiguracaoMembroRequest;
import com.advocacia_microservice.equipe.application.EquipeService;
import com.advocacia_microservice.equipe.domain.*;
import com.advocacia_microservice.equipe.infrastructure.EmailConviteSender;
import com.advocacia_microservice.equipe.infrastructure.persistence.*;
import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.*;
import com.advocacia_microservice.usuario.infrastructure.persistence.JpaUsuarioRepository;
import java.time.*;
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
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test")
class EquipeTests {
    @Autowired CriarUsuarioUseCase criar;
    @Autowired EmpresaService empresas;
    @Autowired EmpresaRepository vinculos;
    @Autowired EquipeService equipe;
    @Autowired CodigoAcessoService codigos;
    @Autowired GoogleLoginService googleLogin;
    @Autowired JpaUsuarioRepository usuarios;
    @Autowired UsuarioPerfilRepository perfis;
    @Autowired ConviteEquipeRepository convites;
    @Autowired WebApplicationContext context;
    @MockitoBean EmailConviteSender email;
    @MockitoBean EmailCodigoSender codigoEmail;
    Usuario master;
    UUID empresaId;
    MockMvc mvc;
    @BeforeEach void preparar() {
        reset(email, codigoEmail);
        master = usuario();
        empresaId = empresas.criar(master.id(), Map.of("razao_social", "Sociedade de teste", "cnpj", UUID.randomUUID().toString().substring(0, 14), "endereco", "Rua A", "cidade", "Brasília")).id();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    Usuario usuario() { return criar.executar("Pessoa", UUID.randomUUID() + "@example.test"); }
    ConfiguracaoMembroRequest configuracao() { return new ConfiguracaoMembroRequest("", "", null, null, PerfilEquipe.ASSISTENTE, SituacaoEquipe.ATIVO, false, Set.of(), false); }
    String configJson(String situacao) {
        return "{\"nomeExibicao\":\"Pessoa\",\"telefone\":\"123\",\"perfil\":\"FINANCEIRO\",\"situacao\":\"" + situacao + "\",\"restrito\":true,\"abas\":[\"agenda\"],\"enviaDocumento\":false}";
    }
    String conviteJson(String email) {
        return "{\"nome\":\"Pessoa\",\"email\":\"" + email + "\",\"configuracao\":{\"nomeExibicao\":\"\",\"telefone\":\"\",\"perfil\":\"ASSISTENTE\",\"situacao\":\"ATIVO\",\"restrito\":false,\"abas\":[],\"enviaDocumento\":false}}";
    }
    Usuario membro() {
        var usuario = usuario();
        equipe.convidar(master.id(), "Pessoa", usuario.email(), configuracao());
        equipe.aceitarConvite(usuario);
        return usuario;
    }

    @Test void criaConviteEConcluiCadastroApenasDepoisDeValidarCodigo() throws Exception {
        var destino = UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/equipe/convites").with(user(master.id().toString())).with(csrf()).contentType("application/json").content(conviteJson(destino)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDENTE"));
        verify(email).enviar(destino, "Pessoa", "Sociedade de teste");
        assertTrue(usuarios.findByEmail(destino).isEmpty());
        var desafio = codigos.solicitar(destino);
        var captor = ArgumentCaptor.forClass(String.class); verify(codigoEmail).enviar(eq(destino), captor.capture());
        mvc.perform(post("/api/auth/cadastro").with(csrf()).contentType("application/json").content("{\"nome\":\"Pessoa\"}"))
                .andExpect(status().isUnauthorized());
        var resultado = mvc.perform(post("/api/auth/validar").with(csrf()).contentType("application/json")
                .content("{\"desafioId\":\"" + desafio + "\",\"codigo\":\"" + captor.getValue() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cadastroPendente").value(true)).andReturn();
        var sessao = (MockHttpSession) resultado.getRequest().getSession(false);
        mvc.perform(post("/api/auth/cadastro").session(sessao).with(csrf()).contentType("application/json").content("{\"nome\":\"Pessoa\"}"))
                .andExpect(status().isOk());
        var conta = usuarios.findByEmail(destino).orElseThrow().paraDominio();
        assertEquals(empresaId, vinculos.vinculo(conta.id()).orElseThrow().empresaId());
        assertEquals(PapelEmpresa.MEMBRO, vinculos.vinculo(conta.id()).orElseThrow().papel());
        assertTrue(equipe.listar(master.id()).convites().isEmpty());
        assertEquals(2, equipe.listar(master.id()).membros().size());
        equipe.aceitarConvite(conta);
        assertEquals(2, equipe.listar(master.id()).membros().size());
    }

    @Test void aceitaConviteDeContaExistenteViaGoogleVerificado() {
        var conta = criar.executar("Google", UUID.randomUUID() + "@gmail.com");
        equipe.convidar(master.id(), conta.nome(), conta.email(), configuracao());
        var google = mock(OidcUser.class);
        when(google.getSubject()).thenReturn(UUID.randomUUID().toString()); when(google.getEmail()).thenReturn(conta.email());
        when(google.getEmailVerified()).thenReturn(false);
        assertThrows(OAuth2AuthenticationException.class, () -> googleLogin.autenticar(google));
        assertTrue(vinculos.vinculo(conta.id()).isEmpty());
        when(google.getEmailVerified()).thenReturn(true);
        assertEquals(conta.id(), googleLogin.autenticar(google).id());
        assertEquals(empresaId, equipe.acesso(conta.id()).empresaId());
    }

    @Test void codigoValidoTambemAceitaConviteDeContaExistente() throws Exception {
        var conta = usuario(); equipe.convidar(master.id(), conta.nome(), conta.email(), configuracao());
        var desafio = codigos.solicitar(conta.email());
        var captor = ArgumentCaptor.forClass(String.class); verify(codigoEmail).enviar(eq(conta.email()), captor.capture());
        String errado = captor.getValue().equals("000000") ? "111111" : "000000";
        mvc.perform(post("/api/auth/validar").with(csrf()).contentType("application/json").content("{\"desafioId\":\"" + desafio + "\",\"codigo\":\"" + errado + "\"}"))
                .andExpect(status().isUnauthorized());
        assertTrue(vinculos.vinculo(conta.id()).isEmpty());
        mvc.perform(post("/api/auth/validar").with(csrf()).contentType("application/json").content("{\"desafioId\":\"" + desafio + "\",\"codigo\":\"" + captor.getValue() + "\"}"))
                .andExpect(status().isOk());
        assertEquals(empresaId, equipe.acesso(conta.id()).empresaId());
    }

    @Test void identidadeGoogleEstavelNaoAceitaConviteDeEnderecoAntigoSemValidarEsseEmail() {
        var conta = criar.executar("Google", UUID.randomUUID() + "@gmail.com");
        var google = mock(OidcUser.class);
        when(google.getSubject()).thenReturn(UUID.randomUUID().toString()); when(google.getEmail()).thenReturn(conta.email()); when(google.getEmailVerified()).thenReturn(true);
        googleLogin.autenticar(google);
        equipe.convidar(master.id(), conta.nome(), conta.email(), configuracao());
        when(google.getEmail()).thenReturn("endereco-atual@gmail.com");
        assertEquals(conta.id(), googleLogin.autenticar(google).id());
        assertTrue(vinculos.vinculo(conta.id()).isEmpty());
        when(google.getEmail()).thenReturn(conta.email()); googleLogin.autenticar(google);
        assertEquals(empresaId, equipe.acesso(conta.id()).empresaId());
    }

    @Test void cancelarEExpirarImpedemAceiteEReenvioRenovaConvite() throws Exception {
        var conta = usuario(); var convite = equipe.convidar(master.id(), conta.nome(), conta.email(), configuracao());
        mvc.perform(delete("/api/equipe/convites/" + convite.id()).with(user(master.id().toString())).with(csrf())).andExpect(status().isNoContent());
        equipe.aceitarConvite(conta); assertTrue(vinculos.vinculo(conta.id()).isEmpty());
        var novo = equipe.convidar(master.id(), conta.nome(), conta.email(), configuracao());
        var entity = convites.findById(novo.id()).orElseThrow(); entity.expiraEm = Instant.now().minusSeconds(1); convites.saveAndFlush(entity);
        equipe.aceitarConvite(conta); assertTrue(vinculos.vinculo(conta.id()).isEmpty());
        assertEquals("EXPIRADO", equipe.listar(master.id()).convites().getFirst().status());
        mvc.perform(post("/api/equipe/convites/" + novo.id() + "/reenviar").with(user(master.id().toString())).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDENTE"));
        equipe.aceitarConvite(conta); assertEquals(empresaId, equipe.acesso(conta.id()).empresaId());
    }

    @Test void erroSmtpNaoDeixaConvitePendenteNoBanco() throws Exception {
        var destino = UUID.randomUUID() + "@example.test";
        doThrow(new MailSendException("SMTP indisponível")).when(email).enviar(eq(destino), anyString(), anyString());
        mvc.perform(post("/api/equipe/convites").with(user(master.id().toString())).with(csrf()).contentType("application/json").content(conviteJson(destino)))
                .andExpect(status().isServiceUnavailable());
        assertTrue(convites.findAll().stream().noneMatch(c -> c.email.equals(destino)));
    }

    @Test void isolaEquipeConvitesEAtualizacoesEntreEmpresas() throws Exception {
        var membro = membro(); var outro = usuario();
        empresas.criar(outro.id(), Map.of("razao_social", "Outra sociedade", "cnpj", UUID.randomUUID().toString().substring(0, 14), "endereco", "Rua B", "cidade", "Brasília"));
        var convite = equipe.convidar(master.id(), "Pessoa", UUID.randomUUID() + "@example.test", configuracao());
        mvc.perform(get("/api/equipe").with(user(outro.id().toString()))).andExpect(status().isOk()).andExpect(jsonPath("$.membros.length()").value(1)).andExpect(jsonPath("$.membros[0].id").value(outro.id().toString()));
        mvc.perform(delete("/api/equipe/convites/" + convite.id()).with(user(outro.id().toString())).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(put("/api/equipe/membros/" + membro.id()).with(user(outro.id().toString())).with(csrf()).contentType("application/json").content(configJson("ATIVO"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/equipe").with(user(membro.id().toString()))).andExpect(status().isForbidden());
        mvc.perform(post("/api/equipe/convites").with(user(membro.id().toString())).with(csrf()).contentType("application/json").content(conviteJson("outro@example.test"))).andExpect(status().isForbidden());
        mvc.perform(put("/api/equipe/membros/" + master.id()).with(user(master.id().toString())).with(csrf()).contentType("application/json").content(configJson("INATIVO"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/equipe/convites").with(user(master.id().toString())).with(csrf()).contentType("application/json").content(conviteJson(outro.email()))).andExpect(status().isConflict());
    }

    @Test void salvaPermissoesTelefoneERevogaSessaoAoSuspender() throws Exception {
        var membro = membro();
        perfis.salvar(new UsuarioPerfil(membro.id(), "", "contato@example.test", "Rua Pessoal", ""));
        mvc.perform(put("/api/equipe/membros/" + membro.id()).with(user(master.id().toString())).with(csrf()).contentType("application/json").content(configJson("ATIVO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.configuracao.telefone").value("123"));
        assertEquals(Set.of("inicio", "config", "agenda"), equipe.acesso(membro.id()).modulos());
        assertEquals("Rua Pessoal", perfis.buscarPorUsuario(membro.id()).orElseThrow().endereco());
        assertEquals("123", perfis.buscarPorUsuario(membro.id()).orElseThrow().telefone());
        mvc.perform(get("/api/financeiro").with(user(membro.id().toString()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/documentos/assinaturas").with(user(membro.id().toString()))).andExpect(status().isForbidden());
        var resultado = mvc.perform(get("/api/equipe/me").with(user(membro.id().toString()))).andExpect(status().isOk()).andReturn();
        var sessao = (MockHttpSession) resultado.getRequest().getSession(false); assertNotNull(sessao);
        mvc.perform(put("/api/equipe/membros/" + membro.id()).with(user(master.id().toString())).with(csrf()).contentType("application/json").content(configJson("SUSPENSO"))).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isUnauthorized());
        assertTrue(sessao.isInvalid());
        assertEquals(StatusUsuario.INATIVO, usuarios.findById(membro.id()).orElseThrow().paraDominio().status());
        mvc.perform(put("/api/equipe/membros/" + membro.id()).with(user(master.id().toString())).with(csrf()).contentType("application/json").content(configJson("ATIVO"))).andExpect(status().isOk());
        mvc.perform(get("/api/equipe/me").with(user(membro.id().toString()))).andExpect(status().isOk());
    }

    @Test void exigeCsrfValidaModuloEDuplicidadeDeConvite() throws Exception {
        var membro = membro(); var destino = UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/equipe/convites").with(user(master.id().toString())).contentType("application/json").content(conviteJson(destino))).andExpect(status().isForbidden());
        mvc.perform(put("/api/equipe/membros/" + membro.id()).with(user(master.id().toString())).with(csrf()).contentType("application/json").content(configJson("ATIVO").replace("agenda", "modulo_inexistente"))).andExpect(status().isBadRequest());
        equipe.convidar(master.id(), "Pessoa", destino, configuracao());
        mvc.perform(post("/api/equipe/convites").with(user(master.id().toString())).with(csrf()).contentType("application/json").content(conviteJson(destino))).andExpect(status().isConflict());
    }
}
