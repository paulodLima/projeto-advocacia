package com.advocacia_microservice.plataforma;

import com.advocacia_microservice.plataforma.application.ProvisionarAdministradorService;
import com.advocacia_microservice.escritorio.application.EscritorioService;
import com.advocacia_microservice.escritorio.infrastructure.persistence.entity.*;
import com.advocacia_microservice.escritorio.infrastructure.persistence.repository.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class EmpresasTests {
    @Autowired WebApplicationContext context;
    @Autowired CriarUsuarioUseCase criar;
    @Autowired ProvisionarAdministradorService provisionar;
    @Autowired EscritorioRepository empresas;
    @Autowired MembroRepository membros;
    @Autowired EscritorioService contexto;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc; UUID admin; UUID comum;
    String endpoint = "/api/plataforma/admin/empresas";
    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String email = UUID.randomUUID() + "@example.com";
        admin = criar.executar("Admin", email).id(); provisionar.executar(email, "Admin");
        comum = criar.executar("Comum", UUID.randomUUID() + "@example.com").id();
    }
    @Test void cadastraEditaListaEAuditaSemVincularAdministrador() throws Exception {
        mvc.perform(post(endpoint).with(user(admin.toString())).with(csrf()).contentType("application/json")
                .content("{\"nome\":\" Empresa A \",\"identificacao\":\"empresa-a\",\"ativo\":true,\"razaoSocial\":\"Empresa Teste Ltda\",\"cnpj\":\"11.222.333/0001-81\",\"quantidadeFuncionarios\":10}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Empresa A"));
        var e = empresas.findAll().getFirst();
        assertTrue(membros.findByUsuarioIdAndAtivoTrue(admin).isEmpty());
        mvc.perform(put(endpoint + "/" + e.id).with(user(admin.toString())).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"Empresa editada\",\"identificacao\":\"empresa-a\",\"ativo\":false,\"razaoSocial\":\"Empresa Teste Ltda\",\"cnpj\":\"11222333000181\",\"quantidadeFuncionarios\":12}"))
                .andExpect(status().isOk());
        mvc.perform(get(endpoint + "/" + e.id).with(user(admin.toString())))
                .andExpect(jsonPath("$.nome").value("Empresa editada")).andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.cnpj").value("11222333000181"))
                .andExpect(jsonPath("$.quantidadeFuncionarios").value(12));
        mvc.perform(get(endpoint).with(user(admin.toString())))
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.itens[0].identificacao").value("empresa-a"));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM plataforma_auditoria WHERE alvo_id = ?", Integer.class, e.id));
    }
    @Test void bloqueiaComumAnonimoESemCsrf() throws Exception {
        mvc.perform(get(endpoint)).andExpect(status().isUnauthorized());
        mvc.perform(get(endpoint).with(user(comum.toString()))).andExpect(status().isForbidden());
        mvc.perform(post(endpoint).with(user(comum.toString())).with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(endpoint + "/" + UUID.randomUUID()).with(user(comum.toString())).with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(endpoint).with(user(admin.toString())).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        assertEquals(0, empresas.count());
    }
    @Test void validaCamposENegaCodigoDuplicado() throws Exception {
        mvc.perform(post(endpoint).with(user(admin.toString())).with(csrf()).contentType("application/json")
                .content("{\"nome\":\" \",\"identificacao\":\"X\",\"ativo\":true,\"razaoSocial\":\"Empresa Teste Ltda\",\"cnpj\":\"11.222.333/0001-81\",\"quantidadeFuncionarios\":10}"))
                .andExpect(status().isBadRequest());
        assertEquals(0, empresas.count());
        String dados = "{\"nome\":\"Empresa\",\"identificacao\":\"empresa-unica\",\"ativo\":true,\"razaoSocial\":\"Empresa Teste Ltda\",\"cnpj\":\"11.222.333/0001-81\",\"quantidadeFuncionarios\":10}";
        mvc.perform(post(endpoint).with(user(admin.toString())).with(csrf()).contentType("application/json").content(dados))
                .andExpect(status().isCreated());
        mvc.perform(post(endpoint).with(user(admin.toString())).with(csrf()).contentType("application/json").content(dados.replace("empresa-unica", "outra-empresa")))
                .andExpect(status().isConflict());
    }
    @Test void empresaInativaNaoPermiteContextoMesmoComVinculo() {
        var acesso = contexto.criar(comum, "Empresa");
        var e = empresas.findById(acesso.id()).orElseThrow(); e.ativo = false; empresas.saveAndFlush(e);
        assertTrue(contexto.listar(comum).isEmpty());
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> contexto.validar(comum, e.id));
    }
    @Test void rejeitaCnpjInvalidoEQuantidadeNegativaSemSalvar() throws Exception {
        String dados = "{\"nome\":\"Empresa\",\"identificacao\":\"empresa-valida\",\"ativo\":true,\"razaoSocial\":\"Empresa Ltda\",\"cnpj\":\"11222333000182\",\"quantidadeFuncionarios\":10}";
        mvc.perform(post(endpoint).with(user(admin.toString())).with(csrf()).contentType("application/json").content(dados))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Informe um CNPJ válido."));
        mvc.perform(post(endpoint).with(user(admin.toString())).with(csrf()).contentType("application/json")
                .content(dados.replace("11222333000182", "11222333000181").replace(":10", ":-1")))
                .andExpect(status().isBadRequest());
        mvc.perform(post(endpoint).with(user(admin.toString())).with(csrf()).contentType("application/json")
                .content(dados.replace("11222333000182", "11222333000181").replace(":10", ":1.5")))
                .andExpect(status().isBadRequest());
        assertEquals(0, empresas.count());
    }
}
