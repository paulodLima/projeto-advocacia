package com.advocacia_microservice.escritorio;

import com.advocacia_microservice.escritorio.application.EscritorioService;
import com.advocacia_microservice.escritorio.infrastructure.persistence.repository.MembroRepository;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EscritorioTests {
    @Autowired WebApplicationContext context;
    @Autowired CriarUsuarioUseCase criar;
    @Autowired EscritorioService service;
    @Autowired MembroRepository membros;
    MockMvc mvc;
    UUID usuario;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        usuario = criar.executar("Teste", UUID.randomUUID() + "@example.com").id();
    }

    @Test
    void criaEscritorioComProprietarioSemDarAcessoAOutros() throws Exception {
        service.criar(usuario, "Meu escritório"); // Apenas fixture da base anterior, sem API pública.
        mvc.perform(post("/api/escritorios").with(user(usuario.toString())).with(csrf())
                .contentType("application/json").content("{\"nome\":\"Empresa indevida\"}"))
                .andExpect(status().isForbidden());
        assertEquals(1, service.listar(usuario).size());
        var outro = criar.executar("Outro", UUID.randomUUID() + "@example.com").id();
        mvc.perform(get("/api/escritorios").with(user(outro.toString())))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void validaContextoERevogaAcessoAoDesativarVinculo() throws Exception {
        var acesso = service.criar(usuario, "A");
        var sessao = new MockHttpSession();
        mvc.perform(put("/api/escritorios/" + acesso.id() + "/contexto")
                .session(sessao).with(user(usuario.toString())).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/escritorios/contexto").session(sessao).with(user(usuario.toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(acesso.id().toString()));
        var membro = membros.findByEscritorioIdAndUsuarioIdAndAtivoTrue(acesso.id(), usuario).orElseThrow();
        membro.ativo = false;
        membros.saveAndFlush(membro);
        mvc.perform(get("/api/escritorios/contexto").session(sessao).with(user(usuario.toString())))
                .andExpect(status().isForbidden());
        assertNull(sessao.getAttribute("escritorio.ativo"));
    }

    @Test
    void naoPermiteSelecionarEscritorioAlheioNemAlterarContaGlobal() throws Exception {
        var outro = criar.executar("Outro", UUID.randomUUID() + "@example.com").id();
        var acesso = service.criar(outro, "B");
        mvc.perform(put("/api/escritorios/" + acesso.id() + "/contexto")
                .with(user(usuario.toString())).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/" + outro).with(user(usuario.toString())))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/usuarios/" + outro).with(user(usuario.toString())).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/" + usuario).with(user(usuario.toString())))
                .andExpect(status().isOk());
    }

    @Test
    void exigeSessaoCsrfEEscritorioSelecionado() throws Exception {
        mvc.perform(get("/api/escritorios")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/escritorios").with(user(usuario.toString()))
                .contentType("application/json").content("{\"nome\":\"A\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/escritorios/contexto").with(user(usuario.toString())))
                .andExpect(status().isForbidden());
    }
}
