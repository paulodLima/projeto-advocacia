package com.advocacia_microservice.caso;

import com.advocacia_microservice.caso.application.CasosService;
import com.advocacia_microservice.crm.application.CrmService;
import com.advocacia_microservice.crm.domain.Lead;
import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.Usuario;
import com.advocacia_microservice.shared.exception.*;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class CasosTests {
    @Autowired CasosService casos;
    @Autowired CrmService crm;
    @Autowired EmpresaService empresas;
    @Autowired EmpresaRepository vinculos;
    @Autowired CriarUsuarioUseCase usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    Usuario master; UUID empresa; MockMvc mvc;
    Usuario usuario(){return usuarios.executar("Teste Casos",UUID.randomUUID()+"@example.test");}
    @Autowired com.advocacia_microservice.plataforma.domain.repository.ConviteInicialRepository convitesIniciais;
    @Autowired com.advocacia_microservice.usuario.domain.UsuarioRepository usuariosDominio;
    UUID empresa(Usuario u){return com.advocacia_microservice.empresa.EmpresaTestSupport.criar(empresas,convitesIniciais,usuariosDominio,u.id(),Map.of("razao_social","Teste Casos","cnpj",UUID.randomUUID().toString().substring(0,14),"endereco","Rua A","cidade","Brasília")).id();}
    @BeforeEach void preparar(){master=usuario();empresa=empresa(master);mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
    CasosService.Formulario form(Integer versao,UUID cliente,CasosService.NovoCliente novo){return new CasosService.Formulario(versao,"Inventário",cliente,null,master.id(),"Parte adversa","Notas",new BigDecimal("1500.50"),LocalDate.of(2026,10,9),novo);}
    CasosService.Caso caso(){return casos.salvar(master.id(),null,form(null,null,null)).caso();}

    @Test void cadastroEdicaoEControleDeVersao(){
        var c=caso();assertNull(c.clienteId());assertEquals("ativo",c.status());assertEquals(new BigDecimal("1500.50"),c.valorProposta());
        var alterado=casos.salvar(master.id(),c.id(),form(c.versao(),null,null)).caso();assertEquals(1,alterado.versao());
        assertThrows(ConflitoException.class,()->casos.salvar(master.id(),c.id(),form(c.versao(),null,null)));
        assertEquals(1,casos.listar(master.id(),"Inventário","ativo",0,24).total());assertEquals(0,casos.listar(master.id(),"%","todos",0,24).total());
    }
    @Test void criaClienteIncompletoComNumerosNormalizadosEDuplicidadeAtomica(){
        var novo=new CasosService.NovoCliente("Maria","123.456.789-01","(61) 99999-1234");
        var c=casos.salvar(master.id(),null,form(null,null,novo)).caso();assertNotNull(c.clienteId());assertEquals("Maria",c.cliente());
        assertEquals("12345678901",jdbc.queryForObject("SELECT valor FROM contato_dado WHERE contato_id=? AND campo='documento'",String.class,c.clienteId()));
        assertEquals("61999991234",jdbc.queryForObject("SELECT valor FROM contato_dado WHERE contato_id=? AND campo='telefone'",String.class,c.clienteId()));
        assertThrows(ConflitoException.class,()->casos.salvar(master.id(),null,form(null,null,novo)));
        assertEquals(1,casos.listar(master.id(),"","todos",0,24).total());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM contato WHERE empresa_id=?",Integer.class,empresa));
        assertNotNull(casos.salvar(master.id(),null,form(null,c.clienteId(),null)).caso().id());
        var invalido=new CasosService.Formulario(null,"Rollback",null,null,UUID.randomUUID(),"","",null,null,new CasosService.NovoCliente("João","98765432100",""));
        assertThrows(IllegalArgumentException.class,()->casos.salvar(master.id(),null,invalido));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM contato WHERE empresa_id=?",Integer.class,empresa));
    }
    @Test void desfechoLimpaCamposEHistoricoMantemAutor(){
        var c=caso();var f=casos.comercial(master.id(),c.id(),new CasosService.Comercial(c.versao(),"fechado",new BigDecimal("1200.00"),"ignorar"));
        assertNotNull(f.caso().fechadoEm());assertEquals("",f.caso().motivoPerda());assertEquals("ativo",f.caso().status());
        f=casos.comercial(master.id(),c.id(),new CasosService.Comercial(f.caso().versao(),"perdido",null,"Preço"));assertNull(f.caso().valorFechado());assertNull(f.caso().fechadoEm());assertEquals("Preço",f.caso().motivoPerda());
        f=casos.anotar(master.id(),c.id(),new CasosService.AnotacaoRequest(f.caso().versao(),LocalDate.now(),"Reunião agendada"));assertEquals(master.nome(),f.anotacoes().getFirst().autor());
        var atual=f;assertThrows(ConflitoException.class,()->casos.anotar(master.id(),c.id(),new CasosService.AnotacaoRequest(0,LocalDate.now(),"Obsoleta")));
        assertThrows(IllegalArgumentException.class,()->casos.comercial(master.id(),c.id(),new CasosService.Comercial(atual.caso().versao(),"fechado",new BigDecimal("-1"),"")));
        assertEquals(1,casos.buscar(master.id(),c.id()).anotacoes().size());
    }
    @Test void encerramentoReaberturaEExclusaoDoCadastroManual(){
        var c=caso();var f=casos.situacao(master.id(),c.id(),new CasosService.SituacaoRequest(c.versao(),"encerrado"));
        assertEquals(0,casos.listar(master.id(),"","ativo",0,24).total());
        var encerrado=f;assertThrows(ConflitoException.class,()->casos.salvar(master.id(),c.id(),form(encerrado.caso().versao(),null,null)));
        f=casos.situacao(master.id(),c.id(),new CasosService.SituacaoRequest(f.caso().versao(),"ativo"));
        f=casos.anotar(master.id(),c.id(),new CasosService.AnotacaoRequest(f.caso().versao(),LocalDate.now(),"Nota"));
        casos.excluir(master.id(),c.id(),f.caso().versao());assertThrows(RecursoNaoEncontradoException.class,()->casos.buscar(master.id(),c.id()));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM caso_anotacao WHERE caso_id=?",Integer.class,c.id()));
    }
    @Test void isolamentoEntreEmpresasEPerfilSomenteLeitura(){
        var c=caso();var outro=usuario();empresa(outro);
        assertThrows(RecursoNaoEncontradoException.class,()->casos.buscar(outro.id(),c.id()));
        assertThrows(RecursoNaoEncontradoException.class,()->casos.situacao(outro.id(),c.id(),new CasosService.SituacaoRequest(0,"encerrado")));
        var leitor=usuario();vinculos.vincular(new VinculoEmpresa(leitor.id(),empresa,PapelEmpresa.MEMBRO));
        jdbc.update("INSERT INTO equipe_membro(usuario_id,perfil,restrito) VALUES (?,'FINANCEIRO',true)",leitor.id());jdbc.update("INSERT INTO equipe_membro_aba(usuario_id,aba) VALUES (?,'casos')",leitor.id());
        assertFalse(casos.opcoes(leitor.id()).podeEditar());assertEquals(c.id(),casos.buscar(leitor.id(),c.id()).caso().id());
        assertThrows(AccessDeniedException.class,()->casos.salvar(leitor.id(),null,form(null,null,null)));
    }
    @Test void casoDoCrmPreservaSnapshotEEvitaExclusao(){
        var l=crm.salvar(master.id(),null,new CrmService.Formulario(null,new Lead.Cadastro("Cliente CRM","61999991234","cliente@example.test","Indicação","Inventário","quente","Observação",List.of())));
        l=crm.registrar(master.id(),l.id(),new CrmService.ContatoRequest(l.versao(),l.dados().entrada(),"WhatsApp","Sem resposta","Retornar amanhã"));
        l=crm.converter(master.id(),l.id(),new CrmService.ConversaoRequest(l.versao(),true));var id=l.dados().casoId();
        var f=casos.buscar(master.id(),id);assertFalse(f.historicoLead().isEmpty());assertEquals(l.id(),f.caso().leadId());assertNotNull(f.caso().clienteId());
        assertThrows(ConflitoException.class,()->casos.excluir(master.id(),id,0));
        casos.salvar(master.id(),id,form(0,f.caso().clienteId(),null));assertEquals(f.historicoLead(),casos.buscar(master.id(),id).historicoLead());
    }
    @Test void endpointsExigemSessaoECsrfEValidamVersao()throws Exception{
        mvc.perform(get("/api/casos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/casos").with(user(master.id().toString())).contentType("application/json").content("{\"titulo\":\"Caso\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/casos").with(user(master.id().toString())).with(csrf()).contentType("application/json").content("{\"titulo\":\"Caso\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.caso.status").value("ativo"));
        var c=caso();mvc.perform(put("/api/casos/"+c.id()+"/comercial").with(user(master.id().toString())).with(csrf()).contentType("application/json").content("{\"versao\":99,\"desfecho\":\"perdido\"}")).andExpect(status().isConflict());
    }
}
