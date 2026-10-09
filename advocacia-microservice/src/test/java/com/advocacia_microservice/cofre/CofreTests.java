package com.advocacia_microservice.cofre;
import com.advocacia_microservice.cofre.application.*;
import com.advocacia_microservice.cofre.application.CofreService.Formulario;
import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.Usuario;
import com.advocacia_microservice.shared.exception.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test")
class CofreTests {
    @Autowired CofreService service;@Autowired CofreCrypto crypto;@Autowired JdbcTemplate jdbc;
    @Autowired EmpresaService empresas;@Autowired EmpresaRepository empresaRepository;@Autowired CriarUsuarioUseCase criarUsuario;@Autowired WebApplicationContext context;
    Usuario master;UUID empresa;MockMvc mvc;
    Usuario usuario() {return criarUsuario.executar("Teste",UUID.randomUUID()+"@example.test");}
    UUID empresa(Usuario u) {return empresas.criar(u.id(),Map.of("razao_social","Teste","cnpj",UUID.randomUUID().toString().substring(0,14),"endereco","Rua A","cidade","Brasília")).id();}
    Formulario form(String senha,boolean admin,Integer versao) {return new Formulario("PJe","https://example.test","login-teste",senha,"Descrição","Observação",admin,versao);}
    @BeforeEach void preparar(){master=usuario();empresa=empresa(master);mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
    @Test void persisteCriptografadoSemSenhaNaListagemEPreservaSenhaAoEditar() throws Exception {
        var c=service.salvar(master.id(),null,form("senha-teste",false,null));
        String cifrado=jdbc.queryForObject("SELECT conteudo FROM cofre_credencial WHERE id=?",String.class,c.id());
        assertFalse(cifrado.contains("senha-teste"));assertFalse(cifrado.contains("login-teste"));
        mvc.perform(get("/api/config/cofre").with(user(master.id().toString()))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.credenciais[0].senha").doesNotExist());
        mvc.perform(post("/api/config/cofre/"+c.id()+"/revelar").with(user(master.id().toString())).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.senha").value("senha-teste")).andExpect(header().string("Cache-Control","no-store"));
        var atualizado=service.salvar(master.id(),c.id(),form(null,false,0));assertEquals(1,atualizado.versao());assertEquals("senha-teste",service.revelar(master.id(),c.id()));
        assertThrows(ConflitoException.class,()->service.excluir(master.id(),c.id(),0));
        service.excluir(master.id(),c.id(),1);assertTrue(service.buscar(master.id()).credenciais().isEmpty());
    }
    @Test void isolaEmpresasEAdminNaoConcedePermissaoDeEscrita() throws Exception {
        var c=service.salvar(master.id(),null,form("senha-teste",true,null));
        var membro=usuario();empresaRepository.vincular(new VinculoEmpresa(membro.id(),empresa,PapelEmpresa.MEMBRO));
        assertTrue(service.buscar(membro.id()).credenciais().isEmpty());assertThrows(RecursoNaoEncontradoException.class,()->service.revelar(membro.id(),c.id()));
        jdbc.update("INSERT INTO equipe_membro(usuario_id,perfil) VALUES (?, 'ADMINISTRADOR')",membro.id());
        assertEquals("senha-teste",service.revelar(membro.id(),c.id()));assertFalse(service.buscar(membro.id()).podeEditar());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.excluir(membro.id(),c.id(),0));
        var outro=usuario();empresa(outro);assertTrue(service.buscar(outro.id()).credenciais().isEmpty());assertThrows(RecursoNaoEncontradoException.class,()->service.revelar(outro.id(),c.id()));
        assertThrows(RecursoNaoEncontradoException.class,()->service.salvar(outro.id(),c.id(),form("outra",false,0)));
        var publico=service.salvar(master.id(),c.id(),form(null,false,0));assertEquals(1,service.buscar(membro.id()).credenciais().size());assertEquals(1,publico.versao());
    }
    @Test void exigeSessaoECsrfInclusiveParaRevelarEValidaFormularios() throws Exception {
        var c=service.salvar(master.id(),null,form("senha",false,null));
        mvc.perform(get("/api/config/cofre")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/config/cofre/"+c.id()+"/revelar").with(user(master.id().toString()))).andExpect(status().isForbidden());
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),null,form("",false,null)));
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),null,new Formulario("Outro","javascript:alert(1)","","senha","","",false,null)));
        assertThrows(ConflitoException.class,()->service.salvar(master.id(),c.id(),form("nova",false,5)));
        assertEquals("senha",service.revelar(master.id(),c.id()));
    }
    @Test void cifraUsaNonceAleatorioEAadERejeitaChaveOuConteudoIncorreto() {
        UUID id=UUID.randomUUID();byte[] texto="segredo-sintetico".getBytes(StandardCharsets.UTF_8);
        String um=crypto.cifrar(empresa,id,texto),dois=crypto.cifrar(empresa,id,texto);assertNotEquals(um,dois);assertArrayEquals(texto,crypto.decifrar(empresa,id,um));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->crypto.decifrar(empresa,UUID.randomUUID(),um));
        byte[] adulterado=Base64.getDecoder().decode(um);adulterado[adulterado.length-1]^=1;
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->crypto.decifrar(empresa,id,Base64.getEncoder().encodeToString(adulterado)));
        byte[] chaveErrada=new byte[32];chaveErrada[0]=1;var outro=new CofreCrypto(Base64.getEncoder().encodeToString(chaveErrada));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->outro.decifrar(empresa,id,um));
        var ausente=new CofreCrypto("");assertFalse(ausente.configurado());assertThrows(org.springframework.web.server.ResponseStatusException.class,()->ausente.cifrar(empresa,id,texto));
    }
}
