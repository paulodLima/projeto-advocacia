package com.advocacia_microservice.caso;

import com.advocacia_microservice.caso.application.*;
import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.Usuario;
import com.advocacia_microservice.shared.exception.*;
import java.time.*;
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
class TarefasCasoTests {
    @Autowired TarefasCasoService tarefas; @Autowired CasosService casos;
    @Autowired EmpresaService empresas; @Autowired EmpresaRepository vinculos;
    @Autowired CriarUsuarioUseCase usuarios; @Autowired JdbcTemplate jdbc; @Autowired WebApplicationContext context;
    Usuario master;UUID empresa;UUID caso;MockMvc mvc;
    Usuario usuario(){return usuarios.executar("Teste Tarefas",UUID.randomUUID()+"@example.test");}
    @Autowired com.advocacia_microservice.plataforma.domain.repository.ConviteInicialRepository convitesIniciais;
    @Autowired com.advocacia_microservice.usuario.domain.UsuarioRepository usuariosDominio;
    UUID empresa(Usuario u){return com.advocacia_microservice.empresa.EmpresaTestSupport.criar(empresas,convitesIniciais,usuariosDominio,u.id(),Map.of("razao_social","Tarefas","cnpj",UUID.randomUUID().toString().substring(0,14),"endereco","Rua A","cidade","Brasília")).id();}
    UUID caso(Usuario u){return casos.salvar(u.id(),null,new CasosService.Formulario(null,"Caso",null,null,null,"","",null,null,null)).caso().id();}
    @BeforeEach void preparar(){master=usuario();empresa=empresa(master);caso=caso(master);mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
    TarefasCasoService.Dados dados(UUID tipo,List<UUID> responsaveis,List<String> flags){return new TarefasCasoService.Dados(tipo,tipo==null?"Revisar contrato":"",LocalDate.of(2026,10,10),LocalDate.of(2026,10,11),LocalTime.of(10,0),LocalTime.of(11,0),LocalDate.of(2026,10,15),"Escritório","Descrição",true,false,"semanal",flags,responsaveis);}
    TarefasCasoService.Tarefa criar(){return tarefas.salvar(master.id(),caso,null,new TarefasCasoService.Formulario(null,dados(null,List.of(master.id()),List.of("Importante"))));}
    Usuario membro(String perfil){var u=usuario();vinculos.vincular(new VinculoEmpresa(u.id(),empresa,PapelEmpresa.MEMBRO));jdbc.update("INSERT INTO equipe_membro(usuario_id,perfil,restrito) VALUES (?,?,true)",u.id(),perfil);jdbc.update("INSERT INTO equipe_membro_aba(usuario_id,aba) VALUES (?,'casos')",u.id());return u;}
    @Test void tiposResponsaveisEVersaoSaoValidadosPorEmpresa(){
        var tipo=UUID.randomUUID();jdbc.update("INSERT INTO cadastro_item(id,empresa_id,tipo,nome,nome_chave,ativo) VALUES (?,?,'tarefas','Revisão','revisão',true)",tipo,empresa);
        var t=tarefas.salvar(master.id(),caso,null,new TarefasCasoService.Formulario(null,dados(tipo,List.of(master.id()),List.of())));assertEquals("Revisão",t.tipo());assertEquals(master.id(),t.responsaveis().getFirst().id());
        var atualizado=tarefas.salvar(master.id(),caso,t.id(),new TarefasCasoService.Formulario(t.versao(),dados(null,List.of(),List.of())));assertEquals(1,atualizado.versao());assertTrue(atualizado.responsaveis().isEmpty());
        assertThrows(ConflitoException.class,()->tarefas.salvar(master.id(),caso,t.id(),new TarefasCasoService.Formulario(0,dados(null,List.of(),List.of()))));
        assertThrows(IllegalArgumentException.class,()->tarefas.salvar(master.id(),caso,null,new TarefasCasoService.Formulario(null,dados(UUID.randomUUID(),List.of(),List.of()))));
        assertEquals(1,tarefas.listar(master.id(),caso).size());
    }
    @Test void cumprimentoEReaberturaPreservamProvidenciasSemDuplicar(){
        var t=criar();var c=tarefas.estado(master.id(),caso,t.id(),new TarefasCasoService.Atualizacao(0,"concluida","Contrato revisado"));assertEquals("concluida",c.status());assertNotNull(c.concluidaEm());assertEquals(master.nome(),c.concluidaPor());assertEquals("Contrato revisado",c.comentarios().getFirst().texto());
        assertThrows(ConflitoException.class,()->tarefas.estado(master.id(),caso,t.id(),new TarefasCasoService.Atualizacao(0,"concluida","Duplicar")));
        var r=tarefas.estado(master.id(),caso,t.id(),new TarefasCasoService.Atualizacao(c.versao(),"pendente",""));assertNull(r.concluidaEm());assertNull(r.concluidaPor());assertEquals(1,r.comentarios().size());
        r=tarefas.comentar(master.id(),caso,t.id(),new TarefasCasoService.ComentarioRequest(r.versao(),"Nova análise"));assertEquals(2,r.comentarios().size());
    }
    @Test void adiamentoMovePeriodoEPrazoFatalEDelegacaoRejeitaMembroExterno(){
        var t=criar();t=tarefas.adiar(master.id(),caso,t.id(),new TarefasCasoService.Adiamento(0,LocalDate.of(2026,10,20)));assertEquals(LocalDate.of(2026,10,21),t.dados().dataFim());assertEquals(LocalDate.of(2026,10,20),t.dados().prazoFatal());
        var m=membro("ADVOGADO");t=tarefas.delegar(master.id(),caso,t.id(),new TarefasCasoService.Delegacao(t.versao(),List.of(m.id())));assertEquals(m.id(),t.responsaveis().getFirst().id());
        var externo=usuario();empresa(externo);var atual=t;assertThrows(IllegalArgumentException.class,()->tarefas.delegar(master.id(),caso,atual.id(),new TarefasCasoService.Delegacao(atual.versao(),List.of(externo.id()))));assertEquals(m.id(),tarefas.buscar(master.id(),caso,t.id()).responsaveis().getFirst().id());
    }
    @Test void privadasSoAparecemParaAutorResponsaveisEMaster(){
        var autor=membro("ADVOGADO");var responsavel=membro("ASSISTENTE");var outro=membro("ADVOGADO");
        var t=tarefas.salvar(autor.id(),caso,null,new TarefasCasoService.Formulario(null,dados(null,List.of(responsavel.id()),List.of("Privada"))));
        assertEquals(1,tarefas.listar(autor.id(),caso).size());assertEquals(1,tarefas.listar(responsavel.id(),caso).size());assertEquals(1,tarefas.listar(master.id(),caso).size());assertTrue(tarefas.listar(outro.id(),caso).isEmpty());assertThrows(RecursoNaoEncontradoException.class,()->tarefas.buscar(outro.id(),caso,t.id()));assertThrows(RecursoNaoEncontradoException.class,()->tarefas.comentar(outro.id(),caso,t.id(),new TarefasCasoService.ComentarioRequest(0,"Não")));
    }
    @Test void outraEmpresaOutroCasoELeitorNaoPodemAlterar(){
        var t=criar();var outro=usuario();empresa(outro);var externo=caso(outro);assertThrows(RecursoNaoEncontradoException.class,()->tarefas.listar(outro.id(),caso));assertThrows(RecursoNaoEncontradoException.class,()->tarefas.buscar(outro.id(),externo,t.id()));
        var interno=caso(master);assertThrows(RecursoNaoEncontradoException.class,()->tarefas.buscar(master.id(),interno,t.id()));
        var leitor=membro("FINANCEIRO");assertEquals(1,tarefas.listar(leitor.id(),caso).size());assertThrows(AccessDeniedException.class,()->tarefas.estado(leitor.id(),caso,t.id(),new TarefasCasoService.Atualizacao(0,"concluida","")));
        casos.situacao(master.id(),caso,new CasosService.SituacaoRequest(0,"encerrado"));assertThrows(ConflitoException.class,()->tarefas.excluir(master.id(),caso,t.id(),0));assertEquals(1,tarefas.listar(master.id(),caso).size());
    }
    @Test void exclusaoRemoveDependenciasEEscritaExigeCsrf()throws Exception{
        var t=criar();t=tarefas.comentar(master.id(),caso,t.id(),new TarefasCasoService.ComentarioRequest(0,"Comentário"));
        mvc.perform(get("/api/casos/"+caso+"/tarefas")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/casos/"+caso+"/tarefas/"+t.id()).param("versao","1").with(user(master.id().toString()))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/casos/"+caso+"/tarefas/"+t.id()).param("versao","0").with(user(master.id().toString())).with(csrf())).andExpect(status().isConflict());
        mvc.perform(delete("/api/casos/"+caso+"/tarefas/"+t.id()).param("versao","1").with(user(master.id().toString())).with(csrf())).andExpect(status().isNoContent());
        assertTrue(tarefas.listar(master.id(),caso).isEmpty());assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM caso_tarefa_comentario WHERE tarefa_id=?",Integer.class,t.id()));
    }
    @Test void rejeitaDatasInvertidasEMantemTarefaAnterior(){
        var t=criar();var d=t.dados();var invalido=new TarefasCasoService.Dados(null,"Erro",d.data(),d.data().minusDays(1),null,null,null,"","",true,false,"nunca",List.of(),List.of());assertThrows(IllegalArgumentException.class,()->tarefas.salvar(master.id(),caso,t.id(),new TarefasCasoService.Formulario(0,invalido)));assertEquals("Revisar contrato",tarefas.buscar(master.id(),caso,t.id()).dados().titulo());
    }
}
