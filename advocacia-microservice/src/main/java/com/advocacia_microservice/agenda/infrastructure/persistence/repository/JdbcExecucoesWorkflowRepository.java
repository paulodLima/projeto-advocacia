package com.advocacia_microservice.agenda.infrastructure.persistence.repository;

import com.advocacia_microservice.agenda.domain.*;
import com.advocacia_microservice.agenda.domain.repository.ExecucoesWorkflowRepository;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcExecucoesWorkflowRepository implements ExecucoesWorkflowRepository {
    private final JdbcTemplate jdbc;
    public JdbcExecucoesWorkflowRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    private ModeloWorkflow modelo(java.sql.ResultSet r,int n) throws java.sql.SQLException {
        return new ModeloWorkflow(r.getObject("id",UUID.class),r.getString("nome"),r.getInt("versao"),r.getInt("buffer"),List.of());
    }
    public List<ModeloWorkflow> modelos(UUID empresa,UUID tipo) {
        var modelos=jdbc.query("SELECT w.* FROM escritorio_workflow w WHERE w.empresa_id=? AND EXISTS(SELECT 1 FROM workflow_gatilho g JOIN cadastro_item t ON t.empresa_id=g.empresa_id AND t.id=g.tarefa_id WHERE g.workflow_id=w.id AND g.empresa_id=w.empresa_id AND g.tarefa_id=? AND t.ativo=TRUE AND t.tipo='tarefas') AND EXISTS(SELECT 1 FROM workflow_etapa e WHERE e.workflow_id=w.id) ORDER BY w.nome_chave,w.id LIMIT 100",this::modelo,empresa,tipo);
        return comEtapas(empresa,modelos);
    }
    public Optional<ModeloWorkflow> modelo(UUID empresa,UUID id) {
        return comEtapas(empresa,jdbc.query("SELECT * FROM escritorio_workflow WHERE empresa_id=? AND id=?",this::modelo,empresa,id)).stream().findFirst();
    }
    private List<ModeloWorkflow> comEtapas(UUID empresa,List<ModeloWorkflow> modelos) {
        if(modelos.isEmpty())return modelos;
        var args=new ArrayList<Object>();args.add(empresa);modelos.forEach(m->args.add(m.id()));
        var etapas=new HashMap<UUID,List<EtapaWorkflow>>();
        jdbc.query("SELECT e.*,COALESCE(NULLIF(m.nome_exibicao,''),u.nome,'') AS responsavel_nome FROM workflow_etapa e JOIN escritorio_workflow w ON w.id=e.workflow_id LEFT JOIN empresa_usuario v ON v.empresa_id=w.empresa_id AND v.usuario_id=e.responsavel LEFT JOIN usuario u ON u.id=v.usuario_id LEFT JOIN equipe_membro m ON m.usuario_id=u.id WHERE w.empresa_id=? AND w.id IN ("+String.join(",",Collections.nCopies(modelos.size(),"?"))+") ORDER BY e.ordem",
            r->{etapas.computeIfAbsent(r.getObject("workflow_id",UUID.class),k->new ArrayList<>()).add(new EtapaWorkflow(r.getObject("id",UUID.class),r.getString("nome"),r.getObject("responsavel",UUID.class),r.getString("responsavel_nome"),r.getInt("dias")));},args.toArray());
        return modelos.stream().map(m->new ModeloWorkflow(m.id(),m.nome(),m.versao(),m.buffer(),etapas.getOrDefault(m.id(),List.of()))).toList();
    }
    public boolean gatilhoAtivo(UUID empresa,UUID workflow,UUID tipo) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM workflow_gatilho g JOIN cadastro_item t ON t.empresa_id=g.empresa_id AND t.id=g.tarefa_id WHERE g.empresa_id=? AND g.workflow_id=? AND g.tarefa_id=? AND t.tipo='tarefas' AND t.ativo=TRUE",Long.class,empresa,workflow,tipo)>0;
    }
    public Optional<ExecucaoWorkflow> porRequisicao(UUID empresa,UUID requisicao) {
        var dados=jdbc.queryForList("SELECT * FROM agenda_workflow_execucao WHERE empresa_id=? AND requisicao_id=?",empresa,requisicao);
        if(dados.isEmpty())return Optional.empty();
        var r=dados.getFirst();var id=(UUID)r.get("id");
        var exclusoes=jdbc.queryForList("SELECT data FROM agenda_workflow_exclusao WHERE empresa_id=? AND execucao_id=? ORDER BY data",LocalDate.class,empresa,id);
        var ids=new ArrayList<UUID>();
        var etapas=jdbc.query("SELECT * FROM agenda_workflow_etapa WHERE empresa_id=? AND execucao_id=? ORDER BY ordem",(rs,n)->{
            ids.add(rs.getObject("atividade_id",UUID.class));
            return new EtapaPlanejada(rs.getObject("modelo_etapa_id",UUID.class),rs.getInt("ordem"),rs.getString("nome"),rs.getObject("responsavel_snapshot",UUID.class),rs.getString("responsavel_nome"),rs.getInt("dias"),rs.getObject("inicio",LocalDate.class),rs.getObject("fim",LocalDate.class));
        },empresa,id);
        var prazo=jdbc.queryForObject("SELECT prazo_fatal FROM agenda_workflow_execucao WHERE empresa_id=? AND id=?",LocalDate.class,empresa,id);
        var plano=new PlanoWorkflow((UUID)r.get("modelo_id"),(String)r.get("nome"),((Number)r.get("modelo_versao")).intValue(),(UUID)r.get("tipo_tarefa_id"),prazo,((Number)r.get("buffer")).intValue(),exclusoes,etapas);
        return Optional.of(new ExecucaoWorkflow(id,(String)r.get("assinatura"),plano,ids));
    }
    public void iniciar(UUID empresa,UUID requisicao,UUID autor,ExecucaoWorkflow e) {
        var p=e.plano();
        jdbc.update("INSERT INTO agenda_workflow_execucao(id,empresa_id,requisicao_id,assinatura,modelo_id,modelo_versao,nome,tipo_tarefa_id,prazo_fatal,buffer,total_etapas,autor_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",e.id(),empresa,requisicao,e.assinatura(),p.workflowId(),p.versao(),p.nome(),p.tipoTarefaId(),p.prazoFatal(),p.buffer(),p.etapas().size(),autor);
        for(var data:p.diasNaoUteis())jdbc.update("INSERT INTO agenda_workflow_exclusao(empresa_id,execucao_id,data) VALUES(?,?,?)",empresa,e.id(),data);
    }
    public void vincularEtapa(UUID empresa,UUID execucao,UUID atividade,EtapaPlanejada e) {
        jdbc.update("INSERT INTO agenda_workflow_etapa(empresa_id,execucao_id,atividade_id,modelo_etapa_id,ordem,nome,responsavel_snapshot,responsavel_nome,dias,inicio,fim) VALUES(?,?,?,?,?,?,?,?,?,?,?)",empresa,execucao,atividade,e.id(),e.ordem(),e.nome(),e.responsavel(),e.responsavelNome(),e.dias(),e.inicio(),e.fim());
    }
}
