package com.advocacia_microservice.parceiro;

import com.advocacia_microservice.parceiro.application.ParceirosService;
import com.advocacia_microservice.parceiro.domain.Parceiro;
import com.advocacia_microservice.cliente.application.ContatosService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test")
class ParceirosTests {
    @Autowired ParceirosService service;
    @Autowired ContatosService contatos;
    @Autowired EmpresaService empresas;
    @Autowired EmpresaRepository vinculos;
    @Autowired CriarUsuarioUseCase usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    Usuario master; UUID empresa; MockMvc mvc;
    Usuario usuario() { return usuarios.executar("Teste", UUID.randomUUID()+"@example.test"); }
    UUID empresa(Usuario u) { return empresas.criar(u.id(), Map.of("razao_social","Teste","cnpj",UUID.randomUUID().toString().substring(0,14),"endereco","Rua A","cidade","Brasília")).id(); }
    @BeforeEach void preparar() { master=usuario(); empresa=empresa(master); mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }
    ParceirosService.Formulario formulario(String nome) { return new ParceirosService.Formulario(null,Map.of("nome",nome,"tipo_pessoa","PF"),List.of(),List.of()); }
    ParceirosService.Formulario de(Parceiro p) { return new ParceirosService.Formulario(p.versao(),p.dados(),p.socios(),p.areasAtuacao()); }
    ParceirosService.Formulario campo(ParceirosService.Formulario f,String k,String v) { var d=new HashMap<>(f.dados());d.put(k,v);return new ParceirosService.Formulario(f.versao(),d,f.socios(),f.areasAtuacao()); }

    @Test void persisteEscritorioSociosAreasEFiltrosSemDuplicarContato() {
        var f=campo(campo(campo(campo(formulario("Escritório Alfa"),"tipo_pessoa","PJ"),"oab","123/DF"),"uf","DF"),"email","TESTE@EXAMPLE.TEST");
        var p=service.salvar(master.id(),null,new ParceirosService.Formulario(null,f.dados(),List.of(new Parceiro.Socio("Sócia","456/DF")),List.of("Cível","Família")));
        var salvo=service.buscar(master.id(),p.id()); assertEquals("Sócia",salvo.socios().getFirst().nome()); assertEquals("teste@example.test",salvo.dados().get("email"));
        assertEquals("Parceiro",contatos.buscar(master.id(),p.id()).dados().get("tipo"));
        assertEquals(1,service.listar(master.id(),"123","DF","Família",0,24).total());
        assertEquals(0,service.listar(master.id(),"%","","",0,24).total());
        assertEquals(List.of("DF"),service.opcoes(master.id()).estados());
        service.salvar(master.id(),null,formulario("Beta"));
        assertEquals(2,service.listar(master.id(),"","","",0,1).total()); assertEquals(1,service.listar(master.id(),"","","",1,1).itens().size());
    }
    @Test void reaproveitaParceiroDeContatosEPreservaDadosExtrasNasDuasTelas() {
        var tag=contatos.criarIndicador(master.id(),"Teste");
        var c=contatos.salvar(master.id(),null,new ContatosService.Formulario(null,Map.of("nome","Ana","tipo","Parceiro","tipo_pessoa","PF","whatsapp","nenhum","carteira","casa","telefone2","(61) 99999-8888"),List.of(),List.of(tag.id())));
        var p=service.buscar(master.id(),c.id()); assertEquals("",p.dados().get("oab"));
        p=service.salvar(master.id(),c.id(),campo(de(p),"oab","222/DF"));
        var atual=contatos.buscar(master.id(),c.id());assertEquals(c.id(),p.id());assertEquals(c.indicadores(),atual.indicadores());assertEquals(c.dados().get("telefone2"),atual.dados().get("telefone2"));
        var d=new HashMap<>(atual.dados());d.put("nome","Ana Maria");
        contatos.salvar(master.id(),c.id(),new ContatosService.Formulario(atual.versao(),d,atual.representantes(),atual.indicadores()));
        var atualizado=service.buscar(master.id(),c.id());assertEquals("Ana Maria",atualizado.dados().get("nome"));assertEquals("222/DF",atualizado.dados().get("oab"));
    }
    @Test void conflitoVersaoEDocumentoPreservamDados() {
        var p=service.salvar(master.id(),null,campo(formulario("Ana"),"documento","123.456.789-01"));
        var novo=service.salvar(master.id(),p.id(),campo(de(p),"nome","Ana Maria"));
        assertThrows(ConflitoException.class,()->service.salvar(master.id(),p.id(),de(p)));
        assertThrows(ConflitoException.class,()->service.excluir(master.id(),p.id(),0));
        assertThrows(ConflitoException.class,()->service.salvar(master.id(),null,campo(formulario("Duplicado"),"documento","123.456.789-01")));
        assertEquals("Ana Maria",service.buscar(master.id(),p.id()).dados().get("nome"));
        var c=contatos.buscar(master.id(),p.id());var d=new HashMap<>(c.dados());d.put("tipo","Fornecedor");
        assertThrows(ConflitoException.class,()->contatos.salvar(master.id(),p.id(),new ContatosService.Formulario(c.versao(),d,List.of(),List.of())));
        service.excluir(master.id(),p.id(),novo.versao());assertThrows(RecursoNaoEncontradoException.class,()->contatos.buscar(master.id(),p.id()));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM contato_evento WHERE contato_id=?",Integer.class,p.id()));
    }
    @Test void carteiraTemVinculosReaisEBloqueiaExclusao() {
        var p=service.salvar(master.id(),null,formulario("Parceiro"));
        var c=contatos.salvar(master.id(),null,new ContatosService.Formulario(null,Map.of("nome","Parte","tipo","Parte interessada","tipo_pessoa","PF","whatsapp","nenhum","carteira","parceiro","carteira_parceiro_id",p.id().toString()),List.of(),List.of()));
        assertEquals(c.id(),service.carteira(master.id(),p.id(),0,24).itens().getFirst().id());
        assertThrows(ConflitoException.class,()->service.excluir(master.id(),p.id(),p.versao()));
        assertEquals(1,service.carteira(master.id(),p.id(),0,24).total());
    }
    @Test void isolaEmpresaETipoEPapeisEPermissaoDaCarteira() {
        var p=service.salvar(master.id(),null,formulario("Parceiro"));var outro=usuario();empresa(outro);
        assertThrows(RecursoNaoEncontradoException.class,()->service.buscar(outro.id(),p.id()));
        assertThrows(RecursoNaoEncontradoException.class,()->service.excluir(outro.id(),p.id(),0));
        var fornecedor=contatos.salvar(master.id(),null,new ContatosService.Formulario(null,Map.of("nome","Fornecedor","tipo","Fornecedor","tipo_pessoa","PF","whatsapp","nenhum","carteira","casa"),List.of(),List.of()));
        assertThrows(RecursoNaoEncontradoException.class,()->service.buscar(master.id(),fornecedor.id()));
        var financeiro=usuario();vinculos.vincular(new VinculoEmpresa(financeiro.id(),empresa,PapelEmpresa.MEMBRO));
        jdbc.update("INSERT INTO equipe_membro(usuario_id,perfil,restrito) VALUES (?,'FINANCEIRO',true)",financeiro.id());
        jdbc.update("INSERT INTO equipe_membro_aba(usuario_id,aba) VALUES (?,'parceiros')",financeiro.id());
        assertEquals(p.id(),service.buscar(financeiro.id(),p.id()).id());assertFalse(service.opcoes(financeiro.id()).podeEditar());
        assertThrows(AccessDeniedException.class,()->service.salvar(financeiro.id(),null,formulario("Proibido")));
        assertThrows(AccessDeniedException.class,()->service.carteira(financeiro.id(),p.id(),0,24));
        jdbc.update("UPDATE equipe_membro SET perfil='ASSISTENTE' WHERE usuario_id=?",financeiro.id());
        assertTrue(service.opcoes(financeiro.id()).podeEditar());
        assertEquals("Novo",service.salvar(financeiro.id(),null,formulario("Novo")).dados().get("nome"));
        assertThrows(AccessDeniedException.class,()->contatos.buscar(financeiro.id(),p.id()));
        assertThrows(AccessDeniedException.class,()->service.buscar(usuario().id(),p.id()));
    }
    @Test void rejeitaCamposInvalidosFotoAreasESociosSemGravar() {
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),null,campo(formulario("Teste"),"site","javascript:alert(1)")));
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),null,campo(formulario("Teste"),"foto_propria","data:image/png;base64,AAAA")));
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),null,new ParceirosService.Formulario(null,formulario("Teste").dados(),List.of(),List.of("Inexistente"))));
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),null,new ParceirosService.Formulario(null,formulario("Teste").dados(),List.of(),Arrays.asList((String)null))));
        assertThrows(IllegalArgumentException.class,()->new Parceiro.Socio("", "123"));
        assertEquals(0,service.listar(master.id(),"","","",0,24).total());
    }
    @Test void exigeSessaoECsrfERespostasSemCache() throws Exception {
        mvc.perform(get("/api/parceiros")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/parceiros").with(user(master.id().toString())).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/parceiros").with(user(master.id().toString())).with(csrf()).contentType("application/json").content("{\"dados\":{\"nome\":\"Parceiro\",\"tipo_pessoa\":\"PF\"},\"socios\":[],\"areasAtuacao\":[]}")).andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store"));
    }
    @Test void persisteFotoRealELimpaSociosAoMudarParaPfPorContatos() throws Exception {
        var imagem=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(imagem,"png",bytes);
        var foto="data:image/png;base64,"+Base64.getEncoder().encodeToString(bytes.toByteArray());
        var f=campo(campo(formulario("Escritório"),"tipo_pessoa","PJ"),"foto_propria",foto);
        var p=service.salvar(master.id(),null,new ParceirosService.Formulario(null,f.dados(),List.of(new Parceiro.Socio("Sócio","123/DF")),List.of("Cível")));
        assertEquals(foto,service.buscar(master.id(),p.id()).dados().get("foto_propria"));
        var c=contatos.buscar(master.id(),p.id());var d=new HashMap<>(c.dados());d.put("tipo_pessoa","PF");
        contatos.salvar(master.id(),p.id(),new ContatosService.Formulario(c.versao(),d,List.of(),List.of()));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM parceiro_socio WHERE contato_id=?",Integer.class,p.id()));
        assertEquals(foto,service.buscar(master.id(),p.id()).dados().get("foto_propria"));
        assertThrows(ConflitoException.class,()->service.salvar(master.id(),p.id(),de(p)));
    }
}
