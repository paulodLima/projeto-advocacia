package com.advocacia_microservice.plataforma;
import com.advocacia_microservice.usuario.infrastructure.persistence.repository.JpaUsuarioRepository;
import com.advocacia_microservice.usuario.infrastructure.persistence.entity.UsuarioEntity;

import com.advocacia_microservice.plataforma.application.ProvisionarAdministradorService;
import com.advocacia_microservice.plataforma.infrastructure.persistence.entity.*;
import com.advocacia_microservice.plataforma.infrastructure.persistence.repository.*;
import com.advocacia_microservice.escritorio.infrastructure.persistence.repository.MembroRepository;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.StatusUsuario;
import com.advocacia_microservice.usuario.infrastructure.persistence.entity.*;
import com.advocacia_microservice.usuario.infrastructure.persistence.repository.*;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlataformaTests {
    @Autowired WebApplicationContext context;
    @Autowired CriarUsuarioUseCase criar;
    @Autowired ProvisionarAdministradorService provisionar;
    @Autowired AdministradorRepository administradores;
    @Autowired JpaUsuarioRepository usuarios;
    @Autowired MembroRepository membros;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;
    UUID usuario;
    String email;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        email = UUID.randomUUID() + "@example.com";
        usuario = criar.executar("Administrador de teste", email).id();
    }

    @Test
    void provisionaUmaVezSemCriarVinculoEmEmpresaENaoReativaRevogado() {
        provisionar.executar(email, "Administrador");
        assertTrue(administradores.existsByUsuarioIdAndAtivoTrue(usuario));
        assertTrue(membros.findByUsuarioIdAndAtivoTrue(usuario).isEmpty());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM plataforma_auditoria WHERE alvo_id = ?", Integer.class, usuario));
        var admin = administradores.findById(usuario).orElseThrow();
        admin.ativo = false;
        administradores.saveAndFlush(admin);
        provisionar.executar(email, "Administrador");
        provisionar.executar("outro@example.com", "Outro");
        assertFalse(administradores.existsByUsuarioIdAndAtivoTrue(usuario));
        assertTrue(usuarios.findByEmail("outro@example.com").isEmpty());
        assertEquals(1, administradores.count());
    }

    @Test
    void configuracaoVaziaNaoConcedeAcessoEInvalidaNaoCriaConta() {
        provisionar.executar("", "");
        assertEquals(0, administradores.count());
        assertThrows(IllegalArgumentException.class, () -> provisionar.executar("email-invalido", "Admin"));
        assertEquals(0, administradores.count());
    }

    @Test
    void administradorEntraSemEmpresaEMembroNaoRecebePrivilegioPorRole() throws Exception {
        mvc.perform(get("/api/plataforma/admin/painel")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/plataforma/admin/painel").with(user(usuario.toString()).roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/plataforma/acesso").with(user(usuario.toString())))
                .andExpect(jsonPath("$.podeAdministrar").value(false));
        provisionar.executar(email, "Admin");
        mvc.perform(get("/api/plataforma/admin/painel").with(user(usuario.toString())))
                .andExpect(status().isOk());
        mvc.perform(get("/api/plataforma/acesso").with(user(usuario.toString())))
                .andExpect(jsonPath("$.podeAdministrar").value(true));
        var admin = administradores.findById(usuario).orElseThrow();
        admin.ativo = false;
        administradores.saveAndFlush(admin);
        mvc.perform(get("/api/plataforma/admin/painel").with(user(usuario.toString())))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorNaoCriaEmpresaPelaApiAntigaNemSeAutopromove() throws Exception {
        provisionar.executar(email, "Admin");
        mvc.perform(post("/api/escritorios").with(user(usuario.toString())).with(csrf())
                .contentType("application/json").content("{\"nome\":\"Indevida\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/plataforma/admin/promover").with(user(usuario.toString())).with(csrf())
                .contentType("application/json").content("{}")).andExpect(status().isNotFound());
        var comum = criar.executar("Comum", UUID.randomUUID() + "@example.com").id();
        mvc.perform(post("/api/plataforma/admin/promover").with(user(comum.toString())).with(csrf())
                .contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }

    @Test
    void contaInativaNaoRecebeBootstrapNemAcessoAdministrativo() throws Exception {
        usuarios.saveAndFlush(UsuarioEntity.de(usuarios.findById(usuario).orElseThrow().paraDominio()
                .atualizar("Admin", email, StatusUsuario.INATIVO)));
        assertThrows(IllegalArgumentException.class, () -> provisionar.executar(email, "Admin"));
        assertEquals(0, administradores.count());
        mvc.perform(get("/api/plataforma/admin/painel").with(user(usuario.toString())))
                .andExpect(status().isUnauthorized());
    }
}
