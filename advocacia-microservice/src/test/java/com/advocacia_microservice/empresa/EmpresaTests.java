package com.advocacia_microservice.empresa;

import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.*;
import com.advocacia_microservice.usuario.infrastructure.persistence.*;
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

@SpringBootTest
@ActiveProfiles("test")
class EmpresaTests {
    @Autowired CriarUsuarioUseCase criarUsuario;
    @Autowired EmpresaService service;
    @Autowired EmpresaRepository repository;
    @Autowired JpaUsuarioRepository usuarios;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    Usuario master;
    String cnpj;

    @BeforeEach void preparar() {
        master = novoUsuario(); cnpj = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    Usuario novoUsuario() { return criarUsuario.executar("Teste", UUID.randomUUID() + "@example.test"); }
    Map<String, String> dados(String cnpj) {
        return Map.of("razao_social", " Sociedade Teste ", "cnpj", cnpj, "endereco", "Rua A", "cidade", "São Paulo", "uf", "sp", "email", "CONTATO@example.test");
    }
    String body(String cnpj) {
        return "{\"dados\":{\"razao_social\":\"Sociedade Teste\",\"cnpj\":\"" + cnpj + "\",\"endereco\":\"Rua A\",\"cidade\":\"São Paulo\",\"uf\":\"sp\",\"email\":\"CONTATO@example.test\"}}";
    }

    @Test void cadastraMasterConsultaAtualizaERestauraDoBanco() throws Exception {
        mvc.perform(get("/api/empresas/minha").with(user(master.id().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").isEmpty());
        mvc.perform(post("/api/empresas").with(user(master.id().toString())).with(csrf())
                .contentType("application/json").content(body(cnpj)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.papel").value("MASTER"))
                .andExpect(jsonPath("$.dados.uf").value("SP"));
        var empresa = service.buscar(master.id());
        assertNotNull(empresa.id());
        assertEquals(empresa.id(), repository.vinculo(master.id()).orElseThrow().empresaId());
        assertEquals("contato@example.test", repository.buscar(empresa.id()).orElseThrow().dados().get("email"));
        mvc.perform(put("/api/empresas/minha").with(user(master.id().toString())).with(csrf())
                .contentType("application/json").content(body(cnpj).replace("Sociedade Teste", "Sociedade Atualizada")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(empresa.id().toString()));
        mvc.perform(get("/api/empresas/minha").with(user(master.id().toString())))
                .andExpect(jsonPath("$.dados.razao_social").value("Sociedade Atualizada"));
        assertEquals(24, repository.buscar(empresa.id()).orElseThrow().dados().size());
        mvc.perform(post("/api/empresas").with(user(master.id().toString())).with(csrf())
                .contentType("application/json").content(body(cnpj))).andExpect(status().isConflict());
    }

    @Test void isolaEmpresasEPermiteSomenteMasterEditar() throws Exception {
        var a = service.criar(master.id(), dados(cnpj));
        var masterB = novoUsuario();
        var b = service.criar(masterB.id(), dados(UUID.randomUUID().toString().substring(0, 14)));
        var membro = novoUsuario();
        repository.vincular(new VinculoEmpresa(membro.id(), a.id(), PapelEmpresa.MEMBRO));
        mvc.perform(get("/api/empresas/minha").with(user(membro.id().toString())))
                .andExpect(jsonPath("$.id").value(a.id().toString())).andExpect(jsonPath("$.papel").value("MEMBRO"));
        mvc.perform(put("/api/empresas/minha").with(user(membro.id().toString())).with(csrf())
                .contentType("application/json").content(body(cnpj))).andExpect(status().isForbidden());
        mvc.perform(get("/api/empresas/minha").with(user(masterB.id().toString())))
                .andExpect(jsonPath("$.id").value(b.id().toString()));
        // IDs enviados no corpo não escolhem a empresa: a sessão continua sendo a fonte.
        mvc.perform(put("/api/empresas/minha").with(user(master.id().toString())).with(csrf())
                .contentType("application/json").content(body(cnpj).replace("{\"dados\":", "{\"empresaId\":\"" + b.id() + "\",\"dados\":")))
                .andExpect(jsonPath("$.id").value(a.id().toString()));
        assertNotEquals(a.id(), b.id());
    }

    @Test void duplicidadeNormalizadaNaoVinculaContaNemCriaOutraEmpresa() throws Exception {
        service.criar(master.id(), dados(cnpj));
        var outro = novoUsuario();
        String formatado = cnpj.substring(0, 2) + "." + cnpj.substring(2);
        mvc.perform(post("/api/empresas").with(user(outro.id().toString())).with(csrf())
                .contentType("application/json").content(body(formatado))).andExpect(status().isConflict());
        assertTrue(repository.vinculo(outro.id()).isEmpty());
    }

    @Test void exigeSessaoCsrfAtivoEVinculoParaAtualizar() throws Exception {
        mvc.perform(get("/api/empresas/minha")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/empresas").with(user(master.id().toString())).contentType("application/json").content(body(cnpj)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/empresas/minha").with(user(master.id().toString())).with(csrf()).contentType("application/json").content(body(cnpj)))
                .andExpect(status().isForbidden());
        usuarios.saveAndFlush(UsuarioEntity.de(master.atualizar(master.nome(), master.email(), StatusUsuario.INATIVO)));
        mvc.perform(get("/api/empresas/minha").with(user(master.id().toString()))).andExpect(status().isUnauthorized());
    }

    @Test void validaCamposSemCriarVinculo() throws Exception {
        for (var body : List.of("{}", "{\"dados\":{}}", body(cnpj).replace("Sociedade Teste", ""),
                body(cnpj).replace("CONTATO@example.test", "invalido"),
                body(cnpj).replace("\"uf\":\"sp\"", "\"campo_injetado\":\"x\""),
                body(cnpj).replace("\"uf\":\"sp\"", "\"constituida_em\":\"2026-02-31\""),
                body(cnpj).replace("\"uf\":\"sp\"", "\"iss_aliquota\":\"101\""))) {
            mvc.perform(post("/api/empresas").with(user(master.id().toString())).with(csrf())
                    .contentType("application/json").content(body)).andExpect(status().isBadRequest());
            assertTrue(repository.vinculo(master.id()).isEmpty());
        }
    }

    @Test void endpointsLegadosDeUsuarioRespeitamVinculoENaoPermitemAlterarMaster() throws Exception {
        var a = service.criar(master.id(), dados(cnpj));
        var outro = novoUsuario(); service.criar(outro.id(), dados(UUID.randomUUID().toString().substring(0, 14)));
        mvc.perform(get("/api/usuarios/" + outro.id()).with(user(master.id().toString()))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/usuarios/" + outro.id()).with(user(master.id().toString())).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/usuarios/" + master.id()).with(user(master.id().toString())).with(csrf())).andExpect(status().isForbidden());
        var email = UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/usuarios").with(user(master.id().toString())).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"Membro\",\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated());
        var membro = usuarios.findByEmail(email).orElseThrow().paraDominio();
        assertEquals(a.id(), repository.vinculo(membro.id()).orElseThrow().empresaId());
        assertEquals(PapelEmpresa.MEMBRO, repository.vinculo(membro.id()).orElseThrow().papel());
        mvc.perform(get("/api/usuarios/" + membro.id()).with(user(master.id().toString()))).andExpect(status().isOk());
        mvc.perform(post("/api/usuarios").with(user(membro.id().toString())).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"Outra\",\"email\":\"outra@example.test\"}"))
                .andExpect(status().isForbidden());
    }
}
