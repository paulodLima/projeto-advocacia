package com.advocacia_microservice.documento;
import com.advocacia_microservice.documento.application.DocumentosConfigService;
import com.advocacia_microservice.documento.domain.PapelTimbrado;
import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.Usuario;
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
class DocumentosConfigTests {
    @Autowired DocumentosConfigService service;
    @Autowired EmpresaService empresas;
    @Autowired EmpresaRepository repository;
    @Autowired CriarUsuarioUseCase criar;
    @Autowired WebApplicationContext context;
    Usuario master; UUID empresa; MockMvc mvc;
    Usuario novo() { return criar.executar("Teste", UUID.randomUUID()+"@example.test"); }
    UUID empresa(Usuario user) { return empresas.criar(user.id(), Map.of("razao_social","Teste","cnpj",UUID.randomUUID().toString().substring(0,14),"endereco","Rua A","cidade","Brasília")).id(); }
    @BeforeEach void preparar() { master=novo(); empresa=empresa(master); mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }
    @Test void salvaPapelEModelosSeparadamentePreservandoCamposVariaveisETextosVazios() throws Exception {
        var papel = new PapelTimbrado("","","direita",true,false,true,false);
        service.salvarPapel(master.id(),papel);
        service.salvarModelos(master.id(),Map.of("recibo:corpo","<p><b>{{cliente}}</b></p>","recibo:rodape",""));
        var config=service.buscar(master.id()); assertEquals(papel,config.documento()); assertEquals("",config.modelos().get("recibo:rodape"));
        assertTrue(config.modelos().get("recibo:corpo").contains("{{cliente}}"));
        service.salvarPapel(master.id(),PapelTimbrado.padrao()); assertEquals(config.modelos(),service.buscar(master.id()).modelos());
        mvc.perform(get("/api/config/documentos").with(user(master.id().toString()))).andExpect(status().isOk()).andExpect(jsonPath("$.papel").value("MASTER"));
    }
    @Test void sanitizaHtmlERecusaDadosInvalidosSemPerderConfiguracaoAnterior() {
        var resultado=service.salvarModelos(master.id(),Map.of("recibo:corpo","<p style='text-align:center' onclick='alert(1)'><b>{{cliente}}</b><script>alert(1)</script><img src='https://example.test/x'><a href='javascript:alert(1)'>texto</a></p>"));
        String html=resultado.get("recibo:corpo"); assertTrue(html.contains("<b>{{cliente}}</b>")); assertTrue(html.contains("text-align:center"));
        assertFalse(html.contains("script")); assertFalse(html.contains("onclick")); assertFalse(html.contains("<img")); assertFalse(html.contains("href"));
        assertThrows(IllegalArgumentException.class,()->service.salvarModelos(master.id(),Map.of("invalido:corpo","texto")));
        assertThrows(IllegalArgumentException.class,()->service.salvarModelos(master.id(),Map.of("recibo:corpo","a".repeat(20001))));
        assertEquals(resultado,service.buscar(master.id()).modelos());
        assertThrows(IllegalArgumentException.class,()->service.salvarPapel(master.id(),new PapelTimbrado("data:image/png;base64,YWJj","","centro",false,true,true,true)));
        assertEquals(PapelTimbrado.padrao(),service.buscar(master.id()).documento());
    }
    @Test void isolaEmpresasBloqueiaMembroEExigeSessaoCsrf() throws Exception {
        service.salvarModelos(master.id(),Map.of("recibo:corpo","Empresa A"));
        var outro=novo(); empresa(outro); assertTrue(service.buscar(outro.id()).modelos().isEmpty());
        var membro=novo(); repository.vincular(new VinculoEmpresa(membro.id(),empresa,PapelEmpresa.MEMBRO));
        assertEquals("Empresa A",service.buscar(membro.id()).modelos().get("recibo:corpo"));
        mvc.perform(put("/api/config/documentos/modelos").with(user(membro.id().toString())).with(csrf()).contentType("application/json").content("{\"modelos\":{}}")) .andExpect(status().isForbidden());
        mvc.perform(put("/api/config/documentos/modelos").with(user(master.id().toString())).contentType("application/json").content("{\"modelos\":{}}")) .andExpect(status().isForbidden());
        mvc.perform(get("/api/config/documentos")).andExpect(status().isUnauthorized());
        var semEmpresa=novo(); assertNull(service.buscar(semEmpresa.id()).empresaId());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.salvarModelos(semEmpresa.id(),Map.of()));
    }
}
