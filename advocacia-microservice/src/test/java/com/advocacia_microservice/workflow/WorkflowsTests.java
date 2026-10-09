package com.advocacia_microservice.workflow;

import com.advocacia_microservice.workflow.application.WorkflowsService;
import com.advocacia_microservice.workflow.application.WorkflowsService.Etapa;
import com.advocacia_microservice.cadastro.application.CadastrosService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test")
class WorkflowsTests {
    @Autowired WorkflowsService service;
    @Autowired CadastrosService cadastros;
    @Autowired EmpresaService empresas;
    @Autowired EmpresaRepository empresaRepository;
    @Autowired CriarUsuarioUseCase criarUsuario;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    Usuario master; UUID empresa, fase, tarefa; MockMvc mvc;
    Usuario usuario() { return criarUsuario.executar("Teste", UUID.randomUUID()+"@example.test"); }
    UUID empresa(Usuario u) { return empresas.criar(u.id(), Map.of("razao_social","Teste","cnpj",UUID.randomUUID().toString().substring(0,14),"endereco","Rua A","cidade","Brasília")).id(); }
    @BeforeEach void preparar() {
        master=usuario(); empresa=empresa(master);
        fase=cadastros.salvar(master.id(),"fases",null,Map.of("nome","Inicial"),true).id();
        tarefa=cadastros.salvar(master.id(),"tarefas",null,Map.of("nome","Petição","fase",fase.toString(),"pontos","1"),true).id();
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @Test void salvaOrdemResponsaveisPrazosEGatilhosPorId() {
        var w=service.criar(master.id()," Petição inicial ");
        var a=new Etapa(UUID.randomUUID()," Revisar ",master.id(),2);
        var b=new Etapa(UUID.randomUUID(),"Protocolar",null,1);
        service.salvar(master.id(),w.id(),w.nome(),3,0,List.of(tarefa),List.of(b,a));
        cadastros.salvar(master.id(),"tarefas",tarefa,Map.of("nome","Petição renomeada","fase",fase.toString(),"pontos","2"),true);
        var dados=service.buscar(master.id()); var salvo=dados.workflows().getFirst();
        assertEquals(List.of(b,a),salvo.etapas()); assertEquals(3,salvo.buffer()); assertEquals(1,salvo.versao());
        assertEquals(List.of(tarefa),salvo.gatilhos()); assertEquals("Petição renomeada",dados.tarefas().getFirst().nome());
        assertEquals(master.id(),dados.responsaveis().getFirst().id());
    }
    @Test void isolaEmpresasEBloqueiaEscritaDeMembros() throws Exception {
        var w=service.criar(master.id(),"Workflow");
        var membro=usuario(); empresaRepository.vincular(new VinculoEmpresa(membro.id(),empresa,PapelEmpresa.MEMBRO));
        mvc.perform(get("/api/config/workflows").with(user(membro.id().toString()))).andExpect(status().isOk()).andExpect(jsonPath("$.workflows[0].id").value(w.id().toString()));
        mvc.perform(post("/api/config/workflows").with(user(membro.id().toString())).with(csrf()).contentType("application/json").content("{\"nome\":\"Novo\"}")).andExpect(status().isForbidden());
        var outro=usuario(); empresa(outro);
        assertTrue(service.buscar(outro.id()).workflows().isEmpty());
        assertThrows(RecursoNaoEncontradoException.class,()->service.salvar(outro.id(),w.id(),"Invadido",0,0,List.of(),List.of()));
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),w.id(),"Workflow",0,0,List.of(),List.of(new Etapa(UUID.randomUUID(),"Etapa",outro.id(),1))));
        var proprio=service.criar(outro.id(),"Outro");
        assertThrows(IllegalArgumentException.class,()->service.salvar(outro.id(),proprio.id(),"Outro",0,0,List.of(tarefa),List.of()));
    }
    @Test void rejeitaInativosDuplicidadesELimitesSemPerderDados() {
        var w=service.criar(master.id(),"Workflow");
        var etapa=new Etapa(UUID.randomUUID(),"Etapa",master.id(),1);
        service.salvar(master.id(),w.id(),w.nome(),0,0,List.of(tarefa),List.of(etapa));
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),w.id(),w.nome(),0,1,List.of(tarefa,tarefa),List.of(etapa)));
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),w.id(),w.nome(),366,1,List.of(tarefa),List.of(etapa)));
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),w.id(),w.nome(),0,1,List.of(tarefa),List.of(etapa,etapa)));
        jdbc.update("UPDATE usuario SET status='INATIVO' WHERE id=?",master.id());
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),w.id(),w.nome(),0,1,List.of(tarefa),List.of(etapa)));
        jdbc.update("UPDATE usuario SET status='ATIVO' WHERE id=?",master.id());
        cadastros.salvar(master.id(),"tarefas",tarefa,Map.of("nome","Petição","fase",fase.toString(),"pontos","1"),false);
        assertThrows(IllegalArgumentException.class,()->service.salvar(master.id(),w.id(),w.nome(),0,1,List.of(tarefa),List.of()));
        assertEquals(List.of(etapa),service.buscar(master.id()).workflows().getFirst().etapas());
        assertThrows(IllegalArgumentException.class,()->new Etapa(UUID.randomUUID(),"Etapa",null,0));
    }
    @Test void controlaVersaoEBloqueiaExclusaoDeTarefaEmUso() throws Exception {
        var w=service.criar(master.id(),"Workflow");
        service.salvar(master.id(),w.id(),w.nome(),0,0,List.of(tarefa),List.of(new Etapa(UUID.randomUUID(),"Etapa",null,1)));
        assertThrows(ConflitoException.class,()->service.salvar(master.id(),w.id(),"Desatualizado",0,0,List.of(),List.of()));
        mvc.perform(delete("/api/config/workflows/"+w.id()).param("versao","0").with(user(master.id().toString())).with(csrf())).andExpect(status().isConflict());
        assertThrows(ConflitoException.class,()->cadastros.excluir(master.id(),"tarefas",tarefa));
        service.excluir(master.id(),w.id(),1); cadastros.excluir(master.id(),"tarefas",tarefa);
        assertTrue(service.buscar(master.id()).workflows().isEmpty());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM workflow_etapa WHERE workflow_id=?",Integer.class,w.id()));
    }
    @Test void exigeSessaoCsrfEVinculoERejeitaNomesDuplicados() throws Exception {
        mvc.perform(get("/api/config/workflows")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/config/workflows").with(user(master.id().toString())).contentType("application/json").content("{\"nome\":\"Novo\"}")).andExpect(status().isForbidden());
        var semEmpresa=usuario(); assertNull(service.buscar(semEmpresa.id()).empresaId());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.criar(semEmpresa.id(),"Novo"));
        service.criar(master.id(),"Workflow");
        mvc.perform(post("/api/config/workflows").with(user(master.id().toString())).with(csrf()).contentType("application/json").content("{\"nome\":\" workflow \"}")).andExpect(status().isConflict());
        mvc.perform(put("/api/config/workflows/"+UUID.randomUUID()).with(user(master.id().toString())).with(csrf()).contentType("application/json").content("{\"nome\":\"Teste\",\"buffer\":0,\"versao\":0,\"gatilhos\":[],\"etapas\":[]}")).andExpect(status().isNotFound());
    }
}
