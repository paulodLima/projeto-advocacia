package com.advocacia_microservice.agenda;

import com.advocacia_microservice.agenda.application.AgendaService;
import com.advocacia_microservice.agenda.application.command.*;
import com.advocacia_microservice.agenda.domain.*;
import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.EmpresaTestSupport;
import com.advocacia_microservice.plataforma.domain.repository.ConviteInicialRepository;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.UsuarioRepository;
import com.advocacia_microservice.cadastro.application.CadastrosService;
import com.advocacia_microservice.shared.exception.*;
import java.time.*;
import java.util.*;
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

@SpringBootTest @ActiveProfiles("test") @Transactional
class AgendaTests {
    @Autowired AgendaService agenda;
    @Autowired CriarUsuarioUseCase criar;
    @Autowired UsuarioRepository usuarios;
    @Autowired EmpresaService empresas;
    @Autowired ConviteInicialRepository convites;
    @Autowired CadastrosService cadastros;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    @Autowired jakarta.persistence.EntityManager em;
    UUID master,empresa;MockMvc mvc;
    static final LocalDate DIA=LocalDate.of(2026,10,15);
    @BeforeEach void preparar() {
        master=usuario("Responsável");empresa=empresa(master);
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    UUID usuario(String nome) {return criar.executar(nome,UUID.randomUUID()+"@example.test").id();}
    UUID empresa(UUID u) {return EmpresaTestSupport.criar(empresas,convites,usuarios,u,Map.of("razao_social","Escritório","cnpj",UUID.randomUUID().toString().substring(0,14),"endereco","Rua A","cidade","Brasília")).id();}
    UUID membro(String perfil) {
        var id=usuario(perfil);jdbc.update("INSERT INTO empresa_usuario(usuario_id,empresa_id,papel) VALUES(?,?,'MEMBRO')",id,empresa);
        jdbc.update("INSERT INTO equipe_membro(usuario_id,perfil) VALUES(?,?)",id,perfil);return id;
    }
    com.advocacia_microservice.cadastro.domain.CadastroRegistro tipoTarefa(UUID dono,String nome) {
        var fase=cadastros.salvar(dono,"fases",null,Map.of("nome","Inicial","codigo","INI"),true);
        return cadastros.salvar(dono,"tarefas",null,Map.of("nome",nome,"fase",fase.id().toString(),"pontos","10"),true);
    }
    SalvarAtividade tarefa(String titulo,LocalDate data,List<UUID> responsaveis) {return new SalvarAtividade(0,TipoAtividade.TAREFA,titulo,null,data,null,null,null,null,true,null,"","Descrição",responsaveis);}
    SalvarAtividade copia(AtividadeAgenda a,LocalDate data) {var d=a.dados();return new SalvarAtividade(a.versao(),d.tipo(),d.titulo(),d.tipoTarefaId(),data,d.dataFim(),d.hora(),d.horaFim(),d.prazoFatal(),d.diaInteiro(),d.modalidade(),d.local(),d.descricao(),d.responsaveis());}
    FiltroAgenda filtro(LocalDate inicio,LocalDate fim,boolean semData) {return new FiltroAgenda(inicio,fim,null,null,null,"",semData);}
    @Test void persisteTarefaSemDataMultiplosResponsaveisETipoConfiguradoPreservandoVinculoAoRenomear() {
        var assistente=membro("ASSISTENTE");
        var tipo=tipoTarefa(master,"Preparar petição");
        var a=agenda.salvar(master,null,new SalvarAtividade(0,TipoAtividade.TAREFA,"",tipo.id(),null,null,null,null,null,true,null,"","",List.of(master,assistente)));
        em.clear();var salvo=agenda.buscar(master,a.id());assertEquals("Preparar petição",salvo.dados().titulo());assertNull(salvo.dados().data());
        assertEquals(Set.of(master,assistente),new HashSet<>(salvo.dados().responsaveis()));
        assertEquals(1,agenda.listar(master,filtro(DIA,DIA,true),0,50).total());
        assertEquals(0,agenda.listar(master,filtro(DIA,DIA,false),0,50).total());
        assertThrows(ConflitoException.class,()->cadastros.excluir(master,"tarefas",tipo.id()));
        cadastros.salvar(master,"tarefas",tipo.id(),Map.of("nome","Preparar recurso","fase",tipo.campos().get("fase"),"pontos","10"),true);
        assertEquals(tipo.id(),agenda.buscar(master,a.id()).dados().tipoTarefaId());
        assertEquals("Preparar petição",agenda.buscar(master,a.id()).dados().titulo());
    }
    @Test void calendarioIncluiCompromissoDeVariosDiasEPrazoFatalForaDaDataPlanejadaSemDuplicarMesmoDia() {
        agenda.salvar(master,null,new SalvarAtividade(0,TipoAtividade.REUNIAO,"Reunião",null,DIA,DIA.plusDays(2),LocalTime.of(9,0),LocalTime.of(10,0),null,false,"ONLINE","https://example.test/reuniao","",List.of(master)));
        agenda.salvar(master,null,new SalvarAtividade(0,TipoAtividade.PRAZO,"Protocolo",null,DIA,null,null,null,DIA.plusDays(4),true,null,"","",List.of(master)));
        var dias=agenda.calendario(master,filtro(DIA,DIA.plusDays(4),false));
        assertEquals(2,dias.getFirst().total());assertEquals(1,dias.get(1).total());assertEquals(1,dias.get(2).total());assertEquals(1,dias.get(4).prazosFatais());
        assertEquals(1,agenda.listar(master,filtro(DIA.plusDays(4),DIA.plusDays(4),false),0,50).total());
        agenda.salvar(master,null,new SalvarAtividade(0,TipoAtividade.PRAZO,"Prazo hoje",null,DIA,null,null,null,DIA,true,null,"","",List.of()));
        assertEquals(3,agenda.calendario(master,filtro(DIA,DIA,false)).getFirst().total());
    }
    @Test void concluirReabrirCancelarPreservaHistoricoEConflitosNaoSobrescrevem() {
        var original=agenda.salvar(master,null,tarefa("Tarefa",DIA,List.of(master)));
        var editada=agenda.salvar(master,original.id(),copia(original,DIA.plusDays(1)));
        assertThrows(ConflitoException.class,()->agenda.salvar(master,original.id(),copia(original,DIA.plusDays(3))));
        assertEquals(DIA.plusDays(1),agenda.buscar(master,original.id()).dados().data());
        var concluida=agenda.alterarSituacao(master,editada.id(),new AlterarSituacaoAgenda(editada.versao(),SituacaoAgenda.CONCLUIDA,"Providência tomada"));
        assertNotNull(concluida.concluidaEm());assertThrows(ConflitoException.class,()->agenda.salvar(master,concluida.id(),copia(concluida,DIA)));
        var reaberta=agenda.alterarSituacao(master,concluida.id(),new AlterarSituacaoAgenda(concluida.versao(),SituacaoAgenda.PENDENTE,"Revisão"));
        assertNull(reaberta.concluidaEm());
        assertThrows(IllegalArgumentException.class,()->agenda.alterarSituacao(master,reaberta.id(),new AlterarSituacaoAgenda(reaberta.versao(),SituacaoAgenda.CANCELADA," ")));
        var cancelada=agenda.alterarSituacao(master,reaberta.id(),new AlterarSituacaoAgenda(reaberta.versao(),SituacaoAgenda.CANCELADA,"Cliente desistiu"));
        assertEquals(5,cancelada.historico().size());assertTrue(cancelada.historico().stream().anyMatch(e->e.observacao().equals("Providência tomada")));
        assertEquals(SituacaoAgenda.CANCELADA,agenda.buscar(master,original.id()).situacao());
    }
    @Test void referenciasDeOutraEmpresaOuInativasSaoRejeitadasSemCriacaoParcial() {
        var outro=usuario("Outro");empresa(outro);var tipo=tipoTarefa(outro,"Tipo externo");
        assertThrows(IllegalArgumentException.class,()->agenda.salvar(master,null,tarefa("Externa",DIA,List.of(outro))));
        assertThrows(IllegalArgumentException.class,()->agenda.salvar(master,null,new SalvarAtividade(0,TipoAtividade.TAREFA,"Tipo externo",tipo.id(),DIA,null,null,null,null,true,null,"","",List.of())));
        var inativo=membro("ASSISTENTE");jdbc.update("UPDATE equipe_membro SET situacao='INATIVO' WHERE usuario_id=?",inativo);em.clear();
        assertThrows(IllegalArgumentException.class,()->agenda.salvar(master,null,tarefa("Inativa",DIA,List.of(inativo))));
        assertEquals(0,agenda.listar(master,filtro(DIA,DIA,true),0,50).total());
    }
    @Test void validacaoDeDatasHorariosPrazoFatalEUrlOnline() {
        assertThrows(IllegalArgumentException.class,()->agenda.salvar(master,null,new SalvarAtividade(0,TipoAtividade.REUNIAO,"Horários",null,DIA,null,LocalTime.of(10,0),LocalTime.of(9,0),null,false,"PRESENCIAL","","",List.of())));
        assertThrows(IllegalArgumentException.class,()->agenda.salvar(master,null,new SalvarAtividade(0,TipoAtividade.AUDIENCIA,"Link",null,DIA,null,null,null,null,true,"ONLINE","javascript:alert(1)","",List.of())));
        assertThrows(IllegalArgumentException.class,()->agenda.salvar(master,null,new SalvarAtividade(0,TipoAtividade.PRAZO,"Prazo",null,DIA,null,null,null,null,true,null,"","",List.of())));
        assertThrows(IllegalArgumentException.class,()->agenda.salvar(master,null,new SalvarAtividade(0,TipoAtividade.TAREFA,"Antes",null,DIA,null,null,null,DIA.minusDays(1),true,null,"","",List.of())));
        assertThrows(IllegalArgumentException.class,()->agenda.listar(master,filtro(DIA,DIA,false),0,101));
        assertThrows(IllegalArgumentException.class,()->agenda.calendario(master,filtro(DIA,DIA.plusDays(62),false)));
    }
    @Test void apiRespeitaEmpresaPerfilModuloESuspensaoComSessao() throws Exception {
        var a=agenda.salvar(master,null,tarefa("Protegida",DIA,List.of()));var outro=usuario("Outro");empresa(outro);
        var url="/api/agenda/"+a.id();
        mvc.perform(get(url).with(user(outro.toString()))).andExpect(status().isNotFound());
        String body="{\"versao\":0,\"situacao\":\"CONCLUIDA\",\"observacao\":\"\"}";
        mvc.perform(patch(url+"/situacao").with(user(outro.toString())).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNotFound());
        var financeiro=membro("FINANCEIRO");
        mvc.perform(get("/api/agenda/opcoes").with(user(financeiro.toString()))).andExpect(status().isOk()).andExpect(jsonPath("$.podeEditar").value(false));
        mvc.perform(patch(url+"/situacao").with(user(financeiro.toString())).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(patch(url+"/situacao").with(user(master.toString())).contentType("application/json").content(body)).andExpect(status().isForbidden());
        jdbc.update("UPDATE empresa_modulo SET liberado=FALSE WHERE empresa_id=? AND modulo_codigo='agenda'",empresa);
        mvc.perform(get(url).with(user(master.toString()))).andExpect(status().isForbidden());
        jdbc.update("UPDATE empresa_modulo SET liberado=TRUE WHERE empresa_id=? AND modulo_codigo='agenda'",empresa);
        jdbc.update("UPDATE escritorio SET ativo=FALSE WHERE id=?",empresa);em.clear();
        mvc.perform(get(url).with(user(master.toString()))).andExpect(status().isUnauthorized());
    }
    @Test void filtrosPaginacaoResponsavelBuscaLiteralEApiIgnoraEmpresaIdDoCliente() throws Exception {
        var assistente=membro("ASSISTENTE");agenda.salvar(master,null,tarefa("Preparar recurso",DIA,List.of(assistente)));
        agenda.salvar(master,null,tarefa("Outras atividades",DIA,List.of(master)));
        var f=new FiltroAgenda(DIA,DIA,TipoAtividade.TAREFA,SituacaoAgenda.PENDENTE,assistente,"ReCuRsO",false);
        assertEquals(1,agenda.listar(master,f,0,1).total());assertEquals(0,agenda.listar(master,f,1,1).itens().size());
        assertEquals(0,agenda.listar(master,new FiltroAgenda(DIA,DIA,null,null,null,"%",false),0,50).total());
        mvc.perform(post("/api/agenda").with(user(master.toString())).with(csrf()).contentType("application/json")
            .content("{\"versao\":0,\"tipo\":\"TAREFA\",\"titulo\":\"API\",\"data\":\"2026-10-15\",\"diaInteiro\":true,\"responsaveis\":[],\"empresaId\":\""+UUID.randomUUID()+"\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.dados.titulo").value("API"));
        assertEquals(3,agenda.listar(master,filtro(DIA,DIA,false),0,50).total());
    }

    UUID contato(UUID escritorio,String nome,String tipo) {
        var id=UUID.randomUUID();
        var registro=new com.advocacia_microservice.cliente.domain.Contato(id,escritorio,0,Map.of("nome",nome,"tipo",tipo,"tipo_pessoa","PF","whatsapp","nenhum","carteira","casa"),List.of(),List.of(),true);
        context.getBean(com.advocacia_microservice.cliente.infrastructure.persistence.repository.ContatoRepository.class).salvar(registro,true);
        return id;
    }
    SalvarAtividade participantes(AtividadeAgenda a,List<UUID> ids) {
        var d=a.dados();
        return new SalvarAtividade(a.versao(),d.tipo(),d.titulo(),d.tipoTarefaId(),d.data(),d.dataFim(),d.hora(),d.horaFim(),d.prazoFatal(),d.diaInteiro(),d.modalidade(),d.local(),d.descricao(),d.responsaveis(),ids);
    }
    @Test void participantesPersistemNaFichaListaSituacaoEEdicaoComHistorico() {
        var cliente=contato(empresa,"Maria","Cliente");var advogado=contato(empresa,"João","Advogado(a)");
        var a=agenda.salvar(master,null,tarefa("Reunião com cliente",DIA,List.of(master)));
        a=agenda.salvar(master,a.id(),participantes(a,List.of(cliente,advogado)));
        em.clear();
        assertEquals(Set.of(cliente,advogado),new HashSet<>(agenda.buscar(master,a.id()).dados().participantes()));
        assertEquals(2,agenda.listar(master,filtro(DIA,DIA,false),0,50).itens().getFirst().dados().participantes().size());
        jdbc.update("UPDATE contato SET nome='Maria Silva' WHERE id=?",cliente);
        assertEquals("Maria Silva",agenda.contatos(master,"",List.of(cliente)).getFirst().nome());
        assertEquals(1,agenda.contatos(master,"Silva",List.of()).size());
        var concluida=agenda.alterarSituacao(master,a.id(),new AlterarSituacaoAgenda(a.versao(),SituacaoAgenda.CONCLUIDA,""));
        assertEquals(2,concluida.dados().participantes().size());assertEquals(List.of(master),concluida.dados().responsaveis());
        assertEquals(3,concluida.historico().size());
        var reaberta=agenda.alterarSituacao(master,a.id(),new AlterarSituacaoAgenda(concluida.versao(),SituacaoAgenda.PENDENTE,""));
        var removida=agenda.salvar(master,a.id(),participantes(reaberta,List.of(cliente)));
        assertEquals(List.of(cliente),removida.dados().participantes());
        assertEquals(DIA,removida.dados().data());
    }
    @Test void participantesRejeitamOutraEmpresaDuplicadosLimitesEConflitosSemSobrescrever() {
        var outro=usuario("Outro");var externa=empresa(outro);var contato=contato(externa,"Externo","Cliente");
        var local=contato(empresa,"Local","Fornecedor");
        var a=agenda.salvar(master,null,tarefa("Protegida",DIA,List.of()));
        assertThrows(IllegalArgumentException.class,()->agenda.salvar(master,a.id(),participantes(a,List.of(contato))));
        assertThrows(IllegalArgumentException.class,()->agenda.salvar(master,a.id(),participantes(a,List.of(local,local))));
        assertThrows(IllegalArgumentException.class,()->agenda.salvar(master,a.id(),participantes(a,java.util.stream.IntStream.range(0,51).mapToObj(i->UUID.randomUUID()).toList())));
        var alterada=agenda.salvar(master,a.id(),participantes(a,List.of(local)));
        assertThrows(ConflitoException.class,()->agenda.salvar(master,a.id(),participantes(a,List.of())));
        assertEquals(List.of(local),agenda.buscar(master,alterada.id()).dados().participantes());
        assertTrue(agenda.contatos(master,"",List.of(contato)).isEmpty());
        assertTrue(agenda.contatos(master,"%",List.of()).isEmpty());
        assertEquals(2,agenda.buscar(master,a.id()).historico().size());
    }
    @Test void semPermissaoContatosOcultaVinculosEPreservaAoEditarOuConcluir() throws Exception {
        var contato=contato(empresa,"Privado","Cliente");
        var original=agenda.salvar(master,null,tarefa("Agenda",DIA,List.of()));
        var a=agenda.salvar(master,original.id(),participantes(original,List.of(contato)));
        jdbc.update("UPDATE empresa_modulo SET liberado=FALSE WHERE empresa_id=? AND modulo_codigo='contatos'",empresa);
        assertFalse(agenda.opcoes(master).podeVincularContatos());
        assertTrue(agenda.buscar(master,a.id()).dados().participantes().isEmpty());
        assertTrue(agenda.listar(master,filtro(DIA,DIA,false),0,50).itens().getFirst().dados().participantes().isEmpty());
        mvc.perform(get("/api/agenda/contatos").with(user(master.toString()))).andExpect(status().isForbidden());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->agenda.salvar(master,a.id(),participantes(a,List.of(contato))));
        var editada=agenda.salvar(master,a.id(),participantes(a,List.of()));
        agenda.alterarSituacao(master,a.id(),new AlterarSituacaoAgenda(editada.versao(),SituacaoAgenda.CONCLUIDA,""));
        jdbc.update("UPDATE empresa_modulo SET liberado=TRUE WHERE empresa_id=? AND modulo_codigo='contatos'",empresa);
        assertEquals(List.of(contato),agenda.buscar(master,a.id()).dados().participantes());
    }
    @Test void exclusaoDeContatoVinculadoRetornaConflitoEAposRemocaoPermiteExcluir() {
        var id=contato(empresa,"Cliente","Cliente");
        var a=agenda.salvar(master,null,tarefa("Agenda",DIA,List.of()));
        a=agenda.salvar(master,a.id(),participantes(a,List.of(id)));
        var contatos=context.getBean(com.advocacia_microservice.cliente.application.ContatosService.class);
        assertThrows(ConflitoException.class,()->contatos.excluir(master,id,0));
        assertEquals(1,agenda.contatos(master,"",List.of(id)).size());
        agenda.salvar(master,a.id(),participantes(a,List.of()));
        contatos.excluir(master,id,0);
        assertTrue(agenda.contatos(master,"",List.of(id)).isEmpty());
    }

    SalvarAtividade repetida(AtividadeAgenda a,RepeticaoAgenda repeticao,LocalDate data) {
        var d=a.dados();
        return new SalvarAtividade(a.versao(),d.tipo(),d.titulo(),d.tipoTarefaId(),data,d.dataFim(),d.hora(),d.horaFim(),d.prazoFatal(),d.diaInteiro(),d.modalidade(),d.local(),d.descricao(),d.responsaveis(),d.participantes(),repeticao);
    }
    AtividadeAgenda sucessora(UUID id) {
        var proxima=jdbc.queryForObject("SELECT id FROM agenda_atividade WHERE empresa_id=? AND anterior_id=?",UUID.class,empresa,id);
        return agenda.buscar(master,proxima);
    }
    @Test void recorrenciaMensalMantemDiaOriginalAposFevereiroEConcluirNovamenteNaoDuplica() {
        var a=agenda.salvar(master,null,tarefa("Mensal",LocalDate.of(2027,1,31),List.of(master)));
        a=agenda.salvar(master,a.id(),repetida(a,RepeticaoAgenda.MENSAL,a.dados().data()));
        var concluida=agenda.alterarSituacao(master,a.id(),new AlterarSituacaoAgenda(a.versao(),SituacaoAgenda.CONCLUIDA,"Feito"));
        var fevereiro=sucessora(a.id());assertEquals(LocalDate.of(2027,2,28),fevereiro.dados().data());
        assertEquals(RepeticaoAgenda.MENSAL,fevereiro.dados().repeticao());
        assertEquals(List.of(master),fevereiro.dados().responsaveis());assertEquals(SituacaoAgenda.PENDENTE,fevereiro.situacao());
        agenda.alterarSituacao(master,fevereiro.id(),new AlterarSituacaoAgenda(fevereiro.versao(),SituacaoAgenda.CONCLUIDA,""));
        assertEquals(LocalDate.of(2027,3,31),sucessora(fevereiro.id()).dados().data());
        var reaberta=agenda.alterarSituacao(master,a.id(),new AlterarSituacaoAgenda(concluida.versao(),SituacaoAgenda.PENDENTE,""));
        agenda.alterarSituacao(master,a.id(),new AlterarSituacaoAgenda(reaberta.versao(),SituacaoAgenda.CONCLUIDA,""));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM agenda_atividade WHERE anterior_id=?",Integer.class,a.id()));
        assertEquals(1,concluida.historico().stream().filter(e->e.acao().equals("RECORRENCIA_GERADA")).count());
        assertEquals(1,agenda.calendario(master,filtro(LocalDate.of(2027,3,31),LocalDate.of(2027,3,31),false)).getFirst().total());
    }
    @Test void recorrenciaPreservaParticipantesSemPermissaoDeContatosEPrazosRelativos() {
        var contato=contato(empresa,"Participante","Cliente");
        var a=agenda.salvar(master,null,new SalvarAtividade(0,TipoAtividade.TAREFA,"Semanal",null,DIA,DIA.plusDays(1),null,null,DIA.plusDays(2),true,null,"Local","Descrição",List.of(master),List.of(contato),RepeticaoAgenda.SEMANAL));
        jdbc.update("UPDATE empresa_modulo SET liberado=FALSE WHERE empresa_id=? AND modulo_codigo='contatos'",empresa);
        agenda.alterarSituacao(master,a.id(),new AlterarSituacaoAgenda(0,SituacaoAgenda.CONCLUIDA,""));
        assertTrue(sucessora(a.id()).dados().participantes().isEmpty());
        jdbc.update("UPDATE empresa_modulo SET liberado=TRUE WHERE empresa_id=? AND modulo_codigo='contatos'",empresa);
        var nova=sucessora(a.id());assertEquals(List.of(contato),nova.dados().participantes());
        assertEquals(DIA.plusDays(7),nova.dados().data());assertEquals(DIA.plusDays(8),nova.dados().dataFim());assertEquals(DIA.plusDays(9),nova.dados().prazoFatal());
    }
    @Test void cancelamentoNaoGeraProximaEDesativarOuMudarDataAtingeSomenteOcorrenciaAtual() {
        var a=agenda.salvar(master,null,tarefa("Diária",DIA,List.of()));
        a=agenda.salvar(master,a.id(),repetida(a,RepeticaoAgenda.DIARIA,DIA));
        var cancelada=agenda.alterarSituacao(master,a.id(),new AlterarSituacaoAgenda(a.versao(),SituacaoAgenda.CANCELADA,"Dispensada"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM agenda_atividade WHERE anterior_id=?",Integer.class,a.id()));
        var reaberta=agenda.alterarSituacao(master,a.id(),new AlterarSituacaoAgenda(cancelada.versao(),SituacaoAgenda.PENDENTE,""));
        var adiada=agenda.salvar(master,a.id(),repetida(reaberta,RepeticaoAgenda.DIARIA,DIA.plusDays(5)));
        agenda.alterarSituacao(master,a.id(),new AlterarSituacaoAgenda(adiada.versao(),SituacaoAgenda.CONCLUIDA,""));
        var proxima=sucessora(a.id());assertEquals(DIA.plusDays(6),proxima.dados().data());
        var encerrada=agenda.salvar(master,proxima.id(),repetida(proxima,RepeticaoAgenda.NUNCA,proxima.dados().data()));
        agenda.alterarSituacao(master,proxima.id(),new AlterarSituacaoAgenda(encerrada.versao(),SituacaoAgenda.CONCLUIDA,""));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM agenda_atividade WHERE anterior_id=?",Integer.class,proxima.id()));
    }
    @Test void repeticaoExigeTarefaDatadaEApiPreservaConfiguracao() throws Exception {
        var semData=agenda.salvar(master,null,tarefa("Sem data",null,List.of()));
        assertThrows(IllegalArgumentException.class,()->agenda.salvar(master,semData.id(),repetida(semData,RepeticaoAgenda.DIARIA,null)));
        mvc.perform(post("/api/agenda").with(user(master.toString())).with(csrf()).contentType("application/json")
            .content("{\"versao\":0,\"tipo\":\"TAREFA\",\"titulo\":\"Anual\",\"data\":\"2028-02-29\",\"diaInteiro\":true,\"responsaveis\":[],\"repeticao\":\"ANUAL\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.dados.repeticao").value("ANUAL"));
        var dados=new DadosAtividade(TipoAtividade.TAREFA,"Anual",null,LocalDate.of(2028,2,29),null,null,null,null,true,null,"","",List.of(),List.of(),RepeticaoAgenda.ANUAL);
        var ciclo=new CicloRecorrencia(dados.data(),0);
        assertEquals(LocalDate.of(2029,2,28),ciclo.proxima(dados).data());
        assertEquals(LocalDate.of(2032,2,29),RepeticaoAgenda.ANUAL.data(dados.data(),4));
        assertThrows(IllegalArgumentException.class,()->new DadosAtividade(TipoAtividade.REUNIAO,"Reunião",null,DIA,null,null,null,null,true,"PRESENCIAL","","",List.of(),List.of(),RepeticaoAgenda.DIARIA));
    }
    @Test void responsavelInativoImpedeConclusaoRecorrenteSemGravacaoParcial() {
        var responsavel=membro("ASSISTENTE");
        var a=agenda.salvar(master,null,tarefa("Diária",DIA,List.of(responsavel)));
        var recorrente=agenda.salvar(master,a.id(),repetida(a,RepeticaoAgenda.DIARIA,DIA));
        jdbc.update("UPDATE equipe_membro SET situacao='INATIVO' WHERE usuario_id=?",responsavel);
        assertThrows(ConflitoException.class,()->agenda.alterarSituacao(master,a.id(),new AlterarSituacaoAgenda(recorrente.versao(),SituacaoAgenda.CONCLUIDA,"")));
        assertEquals(SituacaoAgenda.PENDENTE,agenda.buscar(master,a.id()).situacao());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM agenda_atividade WHERE anterior_id=?",Integer.class,a.id()));
    }
}
