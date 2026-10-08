package com.advocacia_microservice.cliente;

import com.advocacia_microservice.cliente.application.ContatosService;
import com.advocacia_microservice.cliente.application.ContatosService.Formulario;
import com.advocacia_microservice.cliente.domain.Contato;
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
class ContatosTests {
    @Autowired ContatosService service;@Autowired EmpresaService empresas;@Autowired EmpresaRepository vinculos;
    @Autowired CriarUsuarioUseCase usuarios;@Autowired JdbcTemplate jdbc;@Autowired WebApplicationContext context;
    Usuario master;UUID empresa;MockMvc mvc;
    Usuario usuario(){return usuarios.executar("Teste",UUID.randomUUID()+"@example.test");}
    UUID empresa(Usuario u){return empresas.criar(u.id(),Map.of("razao_social","Teste","cnpj",UUID.randomUUID().toString().substring(0,14),"endereco","Rua A","cidade","Brasília")).id();}
    @BeforeEach void preparar(){master=usuario();empresa=empresa(master);mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
    Formulario formulario(String nome,String tipo,Integer versao){return new Formulario(versao,Map.of("nome",nome,"tipo",tipo,"tipo_pessoa","PF","whatsapp","nenhum","carteira","casa"),List.of(),List.of());}
    Formulario alterar(Formulario f,String campo,String valor){var d=new HashMap<>(f.dados());d.put(campo,valor);return new Formulario(f.versao(),d,f.representantes(),f.indicadores());}
    Formulario de(Contato c){return new Formulario(c.versao(),c.dados(),c.representantes(),c.indicadores());}
    @Test void persisteFichaRepresentantesIndicadoresEBuscaPaginada(){
        var tag=service.criarIndicador(master.id(),"Mãe");assertEquals(tag.id(),service.criarIndicador(master.id(),"mãe").id());
        var f=alterar(formulario("Empresa teste","Cliente",null),"tipo_pessoa","PJ");f=alterar(f,"documento","12.345.678/0001-99");f=alterar(f,"telefone","(61) 99999-9999");f=alterar(f,"email","TESTE@EXAMPLE.TEST");
        var c=service.salvar(master.id(),null,new Formulario(null,f.dados(),List.of(Map.of("nome","Representante","cpf","123.456.789-01","cargo","Sócio")),List.of(tag.id())));
        var salvo=service.buscar(master.id(),c.id());assertEquals("teste@example.test",salvo.dados().get("email"));assertEquals("Sócio",salvo.representantes().getFirst().get("cargo"));assertEquals(List.of(tag.id()),salvo.indicadores());
        service.salvar(master.id(),null,formulario("Alfa","Fornecedor",null));
        assertEquals(2,service.listar(master.id(),"","",null,0,1).total());assertEquals(1,service.listar(master.id(),"","",null,1,1).itens().size());
        assertEquals(1,service.listar(master.id(),"teste@example","Cliente",tag.id(),0,24).total());assertEquals(0,service.listar(master.id(),"%","",null,0,24).total());
    }
    @Test void duplicidadeEConflitoNaoApagamDadosOuIndicadores(){
        var f=alterar(formulario("Ana","Fornecedor",null),"documento","123.456.789-01");var c=service.salvar(master.id(),null,f);
        assertThrows(ConflitoException.class,()->service.salvar(master.id(),null,alterar(f,"nome","Outra pessoa")));
        var editado=service.salvar(master.id(),c.id(),alterar(de(c),"nome","Ana Maria"));assertEquals(1,editado.versao());
        assertThrows(ConflitoException.class,()->service.salvar(master.id(),c.id(),de(c)));assertThrows(ConflitoException.class,()->service.excluir(master.id(),c.id(),0));assertEquals("Ana Maria",service.buscar(master.id(),c.id()).dados().get("nome"));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM contato_evento WHERE contato_id=?",Integer.class,c.id()));
        service.excluir(master.id(),c.id(),1);assertThrows(RecursoNaoEncontradoException.class,()->service.buscar(master.id(),c.id()));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM contato_evento WHERE contato_id=?",Integer.class,c.id()));
    }
    @Test void isolaEmpresasEImpedeReferenciasExternas(){
        var c=service.salvar(master.id(),null,formulario("Primeiro","Parceiro",null));var outro=usuario();empresa(outro);
        assertThrows(RecursoNaoEncontradoException.class,()->service.buscar(outro.id(),c.id()));assertThrows(RecursoNaoEncontradoException.class,()->service.excluir(outro.id(),c.id(),0));assertEquals(0,service.listar(outro.id(),"","",null,0,24).total());
        var tag=service.criarIndicador(outro.id(),"Outro");var f=formulario("Cliente","Fornecedor",null);
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),null,new Formulario(null,f.dados(),List.of(),List.of(tag.id()))));
        assertThrows(IllegalArgumentException.class,()->service.salvar(outro.id(),null,alterar(alterar(f,"carteira","parceiro"),"carteira_parceiro_id",c.id().toString())));
        assertThrows(AccessDeniedException.class,()->service.listar(usuario().id(),"","",null,0,24));
    }
    @Test void protegeCarteirasEDaPermissaoDeConsultaAoFinanceiro(){
        var parceiro=service.salvar(master.id(),null,formulario("Parceiro","Parceiro",null));
        service.salvar(master.id(),null,alterar(alterar(formulario("Contato","Fornecedor",null),"carteira","parceiro"),"carteira_parceiro_id",parceiro.id().toString()));
        assertThrows(ConflitoException.class,()->service.excluir(master.id(),parceiro.id(),0));assertThrows(ConflitoException.class,()->service.salvar(master.id(),parceiro.id(),alterar(de(parceiro),"tipo","Fornecedor")));
        var membro=usuario();vinculos.vincular(new VinculoEmpresa(membro.id(),empresa,PapelEmpresa.MEMBRO));jdbc.update("INSERT INTO equipe_membro(usuario_id,perfil) VALUES (?, 'FINANCEIRO')",membro.id());
        assertFalse(service.opcoes(membro.id()).podeEditar());assertEquals(2,service.listar(membro.id(),"","",null,0,24).total());assertThrows(AccessDeniedException.class,()->service.salvar(membro.id(),null,formulario("X","Fornecedor",null)));
        jdbc.update("UPDATE equipe_membro SET restrito=true WHERE usuario_id=?",membro.id());assertThrows(AccessDeniedException.class,()->service.listar(membro.id(),"","",null,0,24));
    }
    @Test void exigeSessaoCsrfEModuloNaApi() throws Exception {
        mvc.perform(get("/api/contatos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/contatos").with(user(master.id().toString())).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/contatos").with(user(master.id().toString()))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        var restrito=usuario();vinculos.vincular(new VinculoEmpresa(restrito.id(),empresa,PapelEmpresa.MEMBRO));jdbc.update("INSERT INTO equipe_membro(usuario_id,perfil,restrito) VALUES (?, 'ASSISTENTE',true)",restrito.id());
        mvc.perform(get("/api/contatos").with(user(restrito.id().toString()))).andExpect(status().isForbidden());
        mvc.perform(post("/api/contatos/indicadores").with(user(master.id().toString())).with(csrf()).contentType("application/json").content("{\"nome\":\"Indicador\"}")).andExpect(status().isCreated());
    }
    @Test void validaCamposDocumentosEEvitaSalvarFormulariosIncompletos(){
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),null,formulario("Cliente","Cliente",null)));
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),null,alterar(formulario("X","Fornecedor",null),"documento","123")));
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),null,alterar(formulario("X","Fornecedor",null),"campo_desconhecido","valor")));
        assertThrows(IllegalArgumentException.class,()->service.listar(master.id(),"","",null,-1,1000));
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),null,alterar(formulario("X","Fornecedor",null),"data_nascimento","3000-01-01")));
        assertEquals(0,service.listar(master.id(),"","",null,0,24).total());
    }
}
