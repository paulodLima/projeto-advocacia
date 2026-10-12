package com.advocacia_microservice.plataforma;
import com.advocacia_microservice.plataforma.application.ProvisionarAdministradorService;
import com.advocacia_microservice.escritorio.application.EscritorioService;
import com.advocacia_microservice.escritorio.infrastructure.persistence.repository.EscritorioRepository;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    @Autowired EscritorioService contexto;
    MockMvc mvc; UUID admin; UUID comum;
    final String endpoint = "/api/plataforma/admin/empresas";
    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String email = UUID.randomUUID() + "@example.test";
        admin = criar.executar("Admin", email).id(); provisionar.executar(email, "Admin");
        comum = criar.executar("Comum", UUID.randomUUID() + "@example.test").id();
    }
    @Test void administradorConsultaMasNaoCadastraOuEditaEmpresaPeloEndpointAntigo() throws Exception {
        var empresa = contexto.criar(comum, "Empresa legada");
        mvc.perform(get(endpoint + "/" + empresa.id()).with(user(admin.toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Empresa legada"));
        var total = empresas.count();
        mvc.perform(post(endpoint).with(user(admin.toString())).with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put(endpoint + "/" + empresa.id()).with(user(admin.toString())).with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed());
        assertEquals(total, empresas.count());
    }
    @Test void consultaAdministrativaBloqueiaUsuarioComumEAnonimo() throws Exception {
        mvc.perform(get(endpoint)).andExpect(status().isUnauthorized());
        mvc.perform(get(endpoint).with(user(comum.toString()))).andExpect(status().isForbidden());
    }
    @Test void empresaInativaNaoPermiteContextoMesmoComVinculo() {
        var acesso = contexto.criar(comum, "Empresa");
        var e = empresas.findById(acesso.id()).orElseThrow(); e.ativo = false; empresas.saveAndFlush(e);
        assertTrue(contexto.listar(comum).isEmpty());
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> contexto.validar(comum, e.id));
    }
}
