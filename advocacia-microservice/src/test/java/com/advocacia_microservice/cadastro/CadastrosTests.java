package com.advocacia_microservice.cadastro;

import com.advocacia_microservice.cadastro.application.CadastrosService;
import com.advocacia_microservice.cadastro.domain.*;
import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.Usuario;
import com.advocacia_microservice.shared.exception.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test")
class CadastrosTests {
    @Autowired CadastrosService service;
    @Autowired EmpresaService empresas;
    @Autowired EmpresaRepository empresaRepository;
    @Autowired CriarUsuarioUseCase criarUsuario;
    @Autowired WebApplicationContext context;
    Usuario master; UUID empresa; MockMvc mvc;
    @BeforeEach void preparar() {
        master = usuario(); empresa = empresa(master);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    Usuario usuario() { return criarUsuario.executar("Teste", UUID.randomUUID() + "@example.test"); }
    UUID empresa(Usuario usuario) { return empresas.criar(usuario.id(), Map.of("razao_social", "Teste", "cnpj", UUID.randomUUID().toString().substring(0, 14), "endereco", "Rua A", "cidade", "Brasília")).id(); }
    CadastroRegistro criar(String tipo, Map<String, String> campos) { return service.salvar(master.id(), tipo, null, campos, true); }
    Sistema sistema(UUID id, String nome, String url) { return new Sistema(id, nome, url, true, "link", "#e4dbd2", ""); }

    @Test void crudDeListasRestauraDoBancoRenomeiaSemPerderReferenciaEBloqueiaExclusaoEmUso() throws Exception {
        var grupo = criar("grupos", Map.of("nome", " Cível ", "sigla", "cv"));
        var acao = criar("acoes", Map.of("nome", "Indenização", "grupo", grupo.id().toString()));
        service.salvar(master.id(), "grupos", grupo.id(), Map.of("nome", "Cível atualizado", "sigla", "cv"), true);
        var restaurado = service.buscar(master.id());
        assertEquals(empresa, restaurado.empresaId()); assertEquals(9, restaurado.listas().size());
        assertEquals("CV", restaurado.listas().get("grupos").getFirst().campos().get("sigla"));
        assertEquals(grupo.id(), restaurado.listas().get("acoes").getFirst().referenciaId());
        mvc.perform(delete("/api/config/cadastros/listas/grupos/" + grupo.id()).with(user(master.id().toString())).with(csrf())).andExpect(status().isConflict());
        service.excluir(master.id(), "acoes", acao.id()); service.excluir(master.id(), "grupos", grupo.id());
        assertTrue(service.buscar(master.id()).listas().get("grupos").isEmpty());
    }
    @Test void fasesEtapasTarefasEtiquetasEValidacoes() throws Exception {
        var fase = criar("fases", Map.of("nome", "Inicial", "codigo", "ini"));
        criar("etapas", Map.of("nome", "Protocolo", "fase", fase.id().toString(), "classificacao", "2"));
        criar("tarefas", Map.of("nome", "Petição", "fase", fase.id().toString(), "pontos", "3.25"));
        criar("etiquetas", Map.of("nome", "Urgente", "cor", "#ff0000"));
        assertEquals("3.25", service.buscar(master.id()).listas().get("tarefas").getFirst().campos().get("pontos"));
        assertThrows(ConflitoException.class, () -> service.excluir(master.id(), "fases", fase.id()));
        assertThrows(IllegalArgumentException.class, () -> criar("tarefas", Map.of("nome", "Erro", "fase", fase.id().toString(), "pontos", "-1")));
        assertThrows(IllegalArgumentException.class, () -> criar("etapas", Map.of("nome", "Erro", "fase", fase.id().toString(), "classificacao", "1.5")));
        assertThrows(IllegalArgumentException.class, () -> criar("acoes", Map.of("nome", "Erro", "grupo", "nome antigo")));
        assertThrows(RecursoNaoEncontradoException.class, () -> criar("acoes", Map.of("nome", "Erro", "grupo", fase.id().toString())));
        mvc.perform(post("/api/config/cadastros/listas/origens").with(user(master.id().toString())).with(csrf()).contentType("application/json").content("{\"campos\":{\"nome\":\"Origem\",\"empresaId\":\"outro\"},\"ativo\":true}"))
            .andExpect(status().isBadRequest());
    }
    @Test void isolaEmpresasEMembroSomenteConsulta() throws Exception {
        var grupo = criar("grupos", Map.of("nome", "Grupo A"));
        var membro = usuario(); empresaRepository.vincular(new VinculoEmpresa(membro.id(), empresa, PapelEmpresa.MEMBRO));
        mvc.perform(get("/api/config/cadastros").with(user(membro.id().toString()))).andExpect(status().isOk()).andExpect(jsonPath("$.papel").value("MEMBRO"));
        mvc.perform(post("/api/config/cadastros/rotinas").with(user(membro.id().toString())).with(csrf()).contentType("application/json").content("{\"nome\":\"Não autorizado\",\"periodo\":\"diaria\"}"))
            .andExpect(status().isForbidden());
        var outro = usuario(); empresa(outro);
        assertTrue(service.buscar(outro.id()).listas().get("grupos").isEmpty());
        assertThrows(RecursoNaoEncontradoException.class, () -> service.salvar(outro.id(), "grupos", grupo.id(), Map.of("nome", "Invadido"), true));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.salvar(outro.id(), "acoes", null, Map.of("nome", "Invadido", "grupo", grupo.id().toString()), true));
    }
    @Test void rotinasDuplicidadeEExclusaoRestritaAEmpresa() throws Exception {
        var rotina = service.criarRotina(master.id(), " Revisar prazos ", "diaria");
        assertEquals("Revisar prazos", service.buscar(master.id()).rotinas().getFirst().nome());
        mvc.perform(post("/api/config/cadastros/rotinas").with(user(master.id().toString())).with(csrf()).contentType("application/json").content("{\"nome\":\"revisar prazos\",\"periodo\":\"diaria\"}"))
            .andExpect(status().isConflict());
        assertThrows(IllegalArgumentException.class, () -> service.criarRotina(master.id(), "Rotina", "invalido"));
        var outro = usuario(); empresa(outro);
        assertThrows(RecursoNaoEncontradoException.class, () -> service.excluirRotina(outro.id(), rotina.id()));
        service.excluirRotina(master.id(), rotina.id()); assertTrue(service.buscar(master.id()).rotinas().isEmpty());
    }
    @Test void sistemasLoteAtomicoIsolamentoUrlsSegurasEOrdem() throws Exception {
        var um = sistema(UUID.randomUUID(), "Primeiro", "https://example.test/primeiro");
        var dois = sistema(UUID.randomUUID(), "Segundo", "http://localhost:9000");
        service.salvarSistemas(master.id(), List.of(um, dois));
        assertEquals(List.of(um.id(), dois.id()), service.buscar(master.id()).sistemas().stream().map(Sistema::id).toList());
        var outro = usuario(); empresa(outro);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.salvarSistemas(outro.id(), List.of(um)));
        assertThrows(IllegalArgumentException.class, () -> service.salvarSistemas(master.id(), List.of(um, um)));
        var logoInvalida = new Sistema(UUID.randomUUID(), "Inválido", "https://example.test", true, "link", "#eeeeee", "data:image/png;base64,YWJj");
        assertThrows(IllegalArgumentException.class, () -> service.salvarSistemas(master.id(), List.of(logoInvalida)));
        assertEquals(2, service.buscar(master.id()).sistemas().size());
        var oculto = new Sistema(dois.id(), dois.nome(), dois.url(), false, dois.icone(), dois.cor(), dois.logo());
        service.salvarSistemas(master.id(), List.of(um, oculto));
        mvc.perform(get("/api/config/cadastros/inicio").with(user(master.id().toString())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.sistemas.length()").value(1)).andExpect(jsonPath("$.listas").doesNotExist());
        for (var url : List.of("javascript:alert(1)", "https://", "https://login:senha@example.test")) assertThrows(IllegalArgumentException.class, () -> sistema(UUID.randomUUID(), "Erro", url));
        service.salvarSistemas(master.id(), List.of(dois)); assertEquals(List.of(dois.id()), service.buscar(master.id()).sistemas().stream().map(Sistema::id).toList());
    }
    @Test void exigeSessaoCsrfEVinculoERejeitaDuplicidadeNormalizada() throws Exception {
        mvc.perform(get("/api/config/cadastros")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/config/cadastros/rotinas").with(user(master.id().toString())).contentType("application/json").content("{\"nome\":\"Rotina\",\"periodo\":\"diaria\"}"))
            .andExpect(status().isForbidden());
        var semEmpresa = usuario(); assertNull(service.buscar(semEmpresa.id()).empresaId());
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.criarRotina(semEmpresa.id(), "Rotina", "diaria"));
        criar("grupos", Map.of("nome", "Grupo"));
        mvc.perform(post("/api/config/cadastros/listas/grupos").with(user(master.id().toString())).with(csrf()).contentType("application/json").content("{\"campos\":{\"nome\":\" grupo \"},\"ativo\":true}"))
            .andExpect(status().isConflict());
    }
}
