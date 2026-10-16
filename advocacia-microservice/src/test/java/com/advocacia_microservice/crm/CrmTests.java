package com.advocacia_microservice.crm;

import com.advocacia_microservice.crm.application.CrmService;
import com.advocacia_microservice.crm.domain.Lead;
import com.advocacia_microservice.cliente.application.ContatosService;
import com.advocacia_microservice.cadastro.application.CadastrosService;
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
class CrmTests {
    @Autowired CrmService crm; @Autowired ContatosService contatos; @Autowired EmpresaService empresas;
    @Autowired EmpresaRepository vinculos; @Autowired CriarUsuarioUseCase usuarios; @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    Usuario master; UUID empresa; MockMvc mvc;
    Usuario usuario() {return usuarios.executar("Teste CRM",UUID.randomUUID()+"@example.test");}
    UUID empresa(Usuario u) {return empresas.criar(u.id(),Map.of("razao_social","Teste CRM","cnpj",UUID.randomUUID().toString().substring(0,14),"endereco","Rua A","cidade","Brasília")).id();}
    @BeforeEach void preparar() {master=usuario();empresa=empresa(master);mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
    Lead.Cadastro cadastro(String nome,List<UUID> tags) {return new Lead.Cadastro(nome,"(61) 99999-1234","LEAD@EXAMPLE.TEST","Indicação","Inventário","quente","Observação",tags);}
    Lead lead() {return crm.salvar(master.id(),null,new CrmService.Formulario(null,cadastro("Interessado",List.of())));}
    UUID etiqueta(UUID empresa,String nome) {
        var id=UUID.randomUUID();jdbc.update("INSERT INTO cadastro_item(id,empresa_id,tipo,nome,nome_chave,ativo) VALUES (?,?,'etiquetas',?,?,true)",id,empresa,nome,nome.toLowerCase(Locale.ROOT));
        jdbc.update("INSERT INTO cadastro_campo(item_id,campo,valor) VALUES (?,'nome',?)",id,nome);jdbc.update("INSERT INTO cadastro_campo(item_id,campo,valor) VALUES (?,'cor','#4f5a49')",id);return id;
    }
    Lead registrar(Lead l,String resultado) {return crm.registrar(master.id(),l.id(),new CrmService.ContatoRequest(l.versao(),l.dados().entrada(),"WhatsApp",resultado,"Ligação combinada"));}
    CrmService.Pagina listar(String busca,String status,String temperatura,boolean atrasados) {return crm.listar(master.id(),busca,status,temperatura,"",atrasados,0,24);}
    @Test void persisteLeadEtiquetasEIndicadoresComFiltrosLiterais() {
        var tag=etiqueta(empresa,"Família");var l=crm.salvar(master.id(),null,new CrmService.Formulario(null,cadastro("Maria",List.of(tag))));
        var salvo=crm.buscar(master.id(),l.id());assertEquals("lead@example.test",salvo.dados().cadastro().email());assertEquals(List.of(tag),salvo.dados().cadastro().etiquetas());
        assertEquals(1,listar("inventário","ativo","quente",true).total());assertEquals(0,listar("%","todos","",false).total());assertEquals(1,listar("","todos","",false).indicadores().emCadencia());
        assertEquals(l.dados().entrada(),l.proximo());
    }
    @Test void avancarCadenciaPausarRetomarEAgendar() {
        var l=registrar(lead(),"Sem resposta");assertEquals(1,l.dados().passo());assertEquals(l.dados().entrada().plusDays(2),l.proximo());
        l=crm.estado(master.id(),l.id(),new CrmService.Atualizacao(l.versao(),new Lead.Estado("ativo",true,null,false,null,null,Map.of())));assertNull(l.proximo());
        l=crm.estado(master.id(),l.id(),new CrmService.Atualizacao(l.versao(),new Lead.Estado("ativo",true,l.dados().entrada().plusDays(10),false,null,null,Map.of())));assertEquals(l.dados().entrada().plusDays(10),l.proximo());
        l=registrar(l,"Quer agendar consultoria");assertEquals("agendou",l.dados().estado().status());assertFalse(l.dados().estado().cadenciaPausada());assertNull(l.proximo());assertEquals(2,l.contatos().size());
    }
    @Test void reguaEsgotadaNaoGeraRetornoEContatoSemInteressePerdeLead() {
        var l=lead();for(int i=0;i<4;i++) l=registrar(l,"Sem resposta");assertEquals(4,l.dados().passo());assertNull(l.proximo());
        l=registrar(l,"Sem interesse");assertEquals("perdido",l.dados().estado().status());assertEquals(0,listar("","ativo","",false).total());assertEquals(1,listar("","perdido","",false).total());
    }
    @Test void consultaCriaAtualizaERemoveEventoDeAgendaEQualificacaoTemTresEstados() {
        var l=lead();var quando=LocalDateTime.of(2026,12,1,10,30);var q=new HashMap<String,Boolean>();q.put("fit",true);q.put("assunto",null);
        l=crm.estado(master.id(),l.id(),new CrmService.Atualizacao(l.versao(),new Lead.Estado("ativo",false,null,true,quando,"marcada",q)));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM agenda_evento WHERE empresa_id=? AND lead_id=?",Integer.class,empresa,l.id()));assertNull(l.dados().estado().qualificacao().get("assunto"));
        var evento=jdbc.queryForObject("SELECT id FROM agenda_evento WHERE lead_id=?",UUID.class,l.id());
        l=crm.estado(master.id(),l.id(),new CrmService.Atualizacao(l.versao(),new Lead.Estado("ativo",false,null,true,quando.plusDays(1),"realizada",Map.of("fit",false))));
        assertEquals("realizada",jdbc.queryForObject("SELECT status FROM agenda_evento WHERE lead_id=?",String.class,l.id()));
        assertEquals(evento,jdbc.queryForObject("SELECT id FROM agenda_evento WHERE lead_id=?",UUID.class,l.id()));
        l=crm.estado(master.id(),l.id(),new CrmService.Atualizacao(l.versao(),Lead.Estado.inicial()));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM agenda_evento WHERE lead_id=?",Integer.class,l.id()));
    }
    @Test void conversaoAtomicaIdempotenteCriaClienteIncompletoECasoComHistorico() {
        var l=registrar(lead(),"Sem resposta");var antes=l;
        l=crm.converter(master.id(),l.id(),new CrmService.ConversaoRequest(l.versao(),true));assertEquals("ganho",l.dados().estado().status());assertNotNull(l.dados().casoId());
        var cliente=jdbc.queryForObject("SELECT cliente_id FROM caso WHERE id=?",UUID.class,l.dados().casoId());assertTrue(contatos.buscar(master.id(),cliente).cadastroIncompleto());
        assertTrue(jdbc.queryForObject("SELECT historico FROM caso WHERE id=?",String.class,l.dados().casoId()).contains("Ligação combinada"));
        assertEquals(l.dados().casoId(),crm.converter(master.id(),antes.id(),new CrmService.ConversaoRequest(antes.versao(),true)).dados().casoId());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM caso WHERE empresa_id=?",Integer.class,empresa));assertThrows(ConflitoException.class,()->crm.excluir(master.id(),antes.id(),antes.versao()+1));
        var c=contatos.buscar(master.id(),cliente);var d=new HashMap<>(c.dados());d.put("documento","12345678901");var completo=contatos.salvar(master.id(),c.id(),new ContatosService.Formulario(c.versao(),d,c.representantes(),c.indicadores()));assertFalse(completo.cadastroIncompleto());
    }
    @Test void conversaoNaoDuplicaClienteERejeitaIdentificacaoAmbiguaSemAlterarLead() {
        var d=Map.of("nome","Cliente","tipo","Cliente","tipo_pessoa","PF","documento","12345678901","telefone","(61) 99999-1234","email","lead@example.test","whatsapp","telefone","carteira","casa");
        var c=contatos.salvar(master.id(),null,new ContatosService.Formulario(null,d,List.of(),List.of()));var l=lead();l=crm.converter(master.id(),l.id(),new CrmService.ConversaoRequest(l.versao(),true));
        assertEquals(c.id(),jdbc.queryForObject("SELECT cliente_id FROM caso WHERE id=?",UUID.class,l.dados().casoId()));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM contato WHERE empresa_id=?",Integer.class,empresa));
        var outro=new HashMap<>(d);outro.put("documento","98765432100");outro.put("email","outro@example.test");contatos.salvar(master.id(),null,new ContatosService.Formulario(null,outro,List.of(),List.of()));
        var novo=lead();assertThrows(ConflitoException.class,()->crm.converter(master.id(),novo.id(),new CrmService.ConversaoRequest(novo.versao(),true)));assertEquals("ativo",crm.buscar(master.id(),novo.id()).dados().estado().status());
    }
    @Test void isolamentoPermissoesVersaoEValidacaoImpedemMutacoesIndevidas() {
        var l=lead();var outro=usuario();var outraEmpresa=empresa(outro);
        assertThrows(RecursoNaoEncontradoException.class,()->crm.buscar(outro.id(),l.id()));assertThrows(RecursoNaoEncontradoException.class,()->crm.converter(outro.id(),l.id(),new CrmService.ConversaoRequest(0,true)));
        var tag=etiqueta(outraEmpresa,"Fora");assertThrows(IllegalArgumentException.class,()->crm.salvar(master.id(),null,new CrmService.Formulario(null,cadastro("Teste",List.of(tag)))));
        var atualizado=registrar(l,"Sem resposta");assertThrows(ConflitoException.class,()->crm.estado(master.id(),l.id(),new CrmService.Atualizacao(l.versao(),Lead.Estado.inicial())));
        assertThrows(IllegalArgumentException.class,()->crm.registrar(master.id(),l.id(),new CrmService.ContatoRequest(atualizado.versao(),l.dados().entrada().plusDays(1),"WhatsApp","Sem resposta","")));
        var leitor=usuario();vinculos.vincular(new VinculoEmpresa(leitor.id(),empresa,PapelEmpresa.MEMBRO));jdbc.update("INSERT INTO equipe_membro(usuario_id,perfil,restrito) VALUES (?,'FINANCEIRO',true)",leitor.id());jdbc.update("INSERT INTO equipe_membro_aba(usuario_id,aba) VALUES (?,'crm')",leitor.id());
        assertFalse(crm.opcoes(leitor.id()).podeEditar());assertEquals(l.id(),crm.buscar(leitor.id(),l.id()).id());assertThrows(AccessDeniedException.class,()->crm.salvar(leitor.id(),null,new CrmService.Formulario(null,cadastro("Não",List.of()))));
    }
    @Test void comentariosMencoesENotificacoesSaoRestritosAEmpresaEDestinatario() {
        var membro=usuario();vinculos.vincular(new VinculoEmpresa(membro.id(),empresa,PapelEmpresa.MEMBRO));var l=lead();l=crm.comentar(master.id(),l.id(),new CrmService.ComentarioRequest(l.versao(),"@Teste CRM confira este lead",List.of(membro.id())));
        assertEquals(1,l.comentarios().size());assertEquals(List.of(membro.id()),l.comentarios().getFirst().mencoes());var n=crm.notificacoes(membro.id());assertEquals(1,n.size());assertTrue(crm.notificacoes(master.id()).isEmpty());
        var id=(UUID)n.getFirst().get("id");assertThrows(RecursoNaoEncontradoException.class,()->crm.lerNotificacao(master.id(),id));crm.lerNotificacao(membro.id(),id);assertTrue(crm.notificacoes(membro.id()).isEmpty());
        var outro=usuario();empresa(outro);var atual=l;assertThrows(IllegalArgumentException.class,()->crm.comentar(master.id(),atual.id(),new CrmService.ComentarioRequest(atual.versao(),"Fora",List.of(outro.id()))));
    }
    @Test void campanhasAgrupamEtiquetasDeLeadsEClientesEMarcamAbordagemPorAno() {
        var tag=etiqueta(empresa,"Família");var l=crm.salvar(master.id(),null,new CrmService.Formulario(null,cadastro("Interessado",List.of(tag))));
        var indicador=contatos.criarIndicador(master.id(),"Família");var d=Map.of("nome","Cliente","tipo","Cliente","tipo_pessoa","PF","documento","12345678901","telefone","(61) 98888-4321","email","cliente@example.test","whatsapp","telefone","carteira","casa");
        contatos.salvar(master.id(),null,new ContatosService.Formulario(null,d,List.of(),List.of(indicador.id())));
        crm.salvarCampanha(master.id(),null,new CrmService.CampanhaRequest(null,"Dia dos Pais",8,"#4f5a49","Campanha",List.of(tag)));int ano=LocalDate.now(ZoneId.of("America/Sao_Paulo")).getYear();
        var c=crm.marketing(master.id(),ano).campanhas().getFirst();assertEquals(2,c.destinatarios().size());crm.abordar(master.id(),c.id(),new CrmService.AbordagemRequest("lead:"+l.id(),ano,true));
        var novas=new com.advocacia_microservice.cadastro.domain.CadastroRegistro(tag,"etiquetas",true,Map.of("nome","Direito de Família","cor","#6a7662"),null);
        new com.advocacia_microservice.cadastro.infrastructure.CadastrosRepository(jdbc).salvar(empresa,novas,false);
        assertEquals(2,crm.marketing(master.id(),ano).campanhas().getFirst().destinatarios().size());assertEquals("Direito de Família",contatos.opcoes(master.id()).indicadores().getFirst().nome());
        assertEquals(1,crm.marketing(master.id(),ano).campanhas().getFirst().destinatarios().stream().filter(CrmService.Destinatario::abordado).count());assertEquals(0,crm.marketing(master.id(),ano+1).campanhas().getFirst().destinatarios().stream().filter(CrmService.Destinatario::abordado).count());
        crm.abordar(master.id(),c.id(),new CrmService.AbordagemRequest("lead:"+l.id(),ano,false));assertFalse(crm.marketing(master.id(),ano).campanhas().getFirst().destinatarios().getFirst().abordado());
        assertThrows(IllegalArgumentException.class,()->crm.abordar(master.id(),c.id(),new CrmService.AbordagemRequest("lead:"+UUID.randomUUID(),ano,true)));
        crm.excluirCampanha(master.id(),c.id(),c.versao());assertTrue(crm.marketing(master.id(),ano).campanhas().isEmpty());
    }
    @Test void cadenciaConfiguradaRecalculaLeadsEProtegeVersoes() {
        var l=lead();crm.salvarCadencia(master.id(),List.of(new Lead.Passo(0,3,"Primeiro"),new Lead.Passo(1,5,"Retorno")));var atual=crm.buscar(master.id(),l.id());assertEquals(l.dados().entrada().plusDays(3),atual.proximo());assertEquals(1,atual.versao());
        assertThrows(ConflitoException.class,()->crm.excluir(master.id(),l.id(),l.versao()));assertThrows(IllegalArgumentException.class,()->crm.salvarCadencia(master.id(),List.of(new Lead.Passo(1,0,"Não"))));
    }
    @Test void exigeSessaoCsrfEValidacaoNoEndpoint() throws Exception {
        mvc.perform(get("/api/crm/leads")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/crm/leads").with(user(master.id().toString())).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/crm/leads").with(user(master.id().toString()))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(post("/api/crm/leads").with(user(master.id().toString())).with(csrf()).contentType("application/json").content("{\"cadastro\":{\"nome\":\"Maria\",\"telefone\":\"61999991234\",\"email\":\"\",\"origem\":\"Outro\",\"necessidade\":\"Inventário\",\"temperatura\":\"morno\",\"observacoes\":\"\",\"etiquetas\":[]}}")).andExpect(status().isCreated());
    }
}
