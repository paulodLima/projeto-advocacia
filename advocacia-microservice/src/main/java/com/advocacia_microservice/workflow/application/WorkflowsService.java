package com.advocacia_microservice.workflow.application;

import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.PapelEmpresa;
import com.advocacia_microservice.shared.exception.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service @Transactional(readOnly=true)
public class WorkflowsService {
    public record Etapa(UUID id, String nome, UUID responsavel, int dias) {
        public Etapa { if(id==null || nome==null || nome.isBlank() || nome.strip().length()>150 || dias<1 || dias>365) throw new IllegalArgumentException("Informe o nome da etapa e de 1 a 365 dias úteis."); nome=nome.strip(); }
    }
    public record Workflow(UUID id,String nome,int buffer,int versao,List<UUID> gatilhos,List<Etapa> etapas) {}
    public record Opcao(UUID id,String nome,boolean ativo) {}
    public record Dados(UUID empresaId,PapelEmpresa papel,List<Workflow> workflows,List<Opcao> tarefas,List<Opcao> responsaveis) {}
    private final JdbcTemplate jdbc; private final EmpresaService empresas;
    public WorkflowsService(JdbcTemplate jdbc,EmpresaService empresas) { this.jdbc=jdbc; this.empresas=empresas; }
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public Dados buscar(UUID usuario) {
        var empresa=empresas.buscar(usuario);
        if(empresa.id()==null) return new Dados(null,null,List.of(),List.of(),List.of());
        UUID id=empresa.id();
        var etapas=new HashMap<UUID,List<Etapa>>(); var gatilhos=new HashMap<UUID,List<UUID>>();
        jdbc.query("SELECT e.* FROM workflow_etapa e JOIN escritorio_workflow w ON w.id=e.workflow_id WHERE w.empresa_id=? ORDER BY e.ordem",rs->{ etapas.computeIfAbsent(rs.getObject("workflow_id",UUID.class),k->new ArrayList<>()).add(new Etapa(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getObject("responsavel",UUID.class),rs.getInt("dias"))); },id);
        jdbc.query("SELECT * FROM workflow_gatilho WHERE empresa_id=? ORDER BY tarefa_id",rs->{gatilhos.computeIfAbsent(rs.getObject("workflow_id",UUID.class),k->new ArrayList<>()).add(rs.getObject("tarefa_id",UUID.class));},id);
        var workflows=jdbc.query("SELECT * FROM escritorio_workflow WHERE empresa_id=? ORDER BY nome_chave,id",(rs,n)->new Workflow(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getInt("buffer"),rs.getInt("versao"),gatilhos.getOrDefault(rs.getObject("id",UUID.class),List.of()),etapas.getOrDefault(rs.getObject("id",UUID.class),List.of())),id);
        var tarefas=jdbc.query("SELECT id,nome,ativo FROM cadastro_item WHERE empresa_id=? AND tipo='tarefas' ORDER BY nome_chave",(rs,n)->new Opcao(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getBoolean("ativo")),id);
        var responsaveis=jdbc.query("SELECT u.id, CASE WHEN m.nome_exibicao IS NOT NULL AND m.nome_exibicao<>'' THEN m.nome_exibicao ELSE u.nome END AS nome, CASE WHEN u.status='ATIVO' AND (m.situacao IS NULL OR m.situacao='ATIVO') THEN true ELSE false END AS ativo FROM empresa_usuario v JOIN usuario u ON u.id=v.usuario_id LEFT JOIN equipe_membro m ON m.usuario_id=u.id WHERE v.empresa_id=? ORDER BY u.nome",(rs,n)->new Opcao(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getBoolean("ativo")),id);
        return new Dados(id,empresa.papel(),workflows,tarefas,responsaveis);
    }
    private UUID editar(UUID usuario) { UUID empresa=empresas.exigirMaster(usuario).empresaId(); jdbc.queryForObject("SELECT id FROM empresa WHERE id=? FOR UPDATE",UUID.class,empresa);return empresa; }
    private String nome(String valor) { if(valor==null||valor.isBlank()||valor.strip().length()>150) throw new IllegalArgumentException("Informe um nome de até 150 caracteres.");return valor.strip(); }
    @Transactional public Workflow criar(UUID usuario,String nome) {
        UUID empresa=editar(usuario),id=UUID.randomUUID(); nome=nome(nome);
        jdbc.update("INSERT INTO escritorio_workflow(id,empresa_id,nome,nome_chave,buffer,versao) VALUES (?,?,?,?,0,0)",id,empresa,nome,nome.toLowerCase(Locale.ROOT));
        return new Workflow(id,nome,0,0,List.of(),List.of());
    }
    @Transactional public Workflow salvar(UUID usuario,UUID id,String nome,int buffer,int versao,List<UUID> gatilhos,List<Etapa> etapas) {
        UUID empresa=editar(usuario); nome=nome(nome);
        var atuais=buscar(usuario); var anterior=atuais.workflows().stream().filter(w->w.id().equals(id)).findFirst().orElseThrow(()->new RecursoNaoEncontradoException("Workflow não encontrado na sua empresa."));
        if(anterior.versao()!=versao) throw new ConflitoException("Este workflow foi atualizado em outra sessão. Reabra a tela antes de salvar.");
        if(buffer<0||buffer>365||gatilhos==null||etapas==null||gatilhos.size()>100||etapas.size()>100||gatilhos.stream().anyMatch(Objects::isNull)||etapas.stream().anyMatch(Objects::isNull)||new HashSet<>(gatilhos).size()!=gatilhos.size()||etapas.stream().map(Etapa::id).distinct().count()!=etapas.size()) throw new IllegalArgumentException("Informe até 100 etapas e gatilhos sem duplicidade e buffer de 0 a 365 dias.");
        for(var gatilho:gatilhos) if(atuais.tarefas().stream().noneMatch(t->t.id().equals(gatilho)&&t.ativo())) throw new IllegalArgumentException("Selecione um tipo de tarefa ativo da sua empresa.");
        for(var etapa:etapas) if(etapa.responsavel()!=null && atuais.responsaveis().stream().noneMatch(r->r.id().equals(etapa.responsavel())&&r.ativo())) throw new IllegalArgumentException("Selecione um responsável ativo da sua empresa ou deixe a etapa sem responsável.");
        jdbc.update("UPDATE escritorio_workflow SET nome=?,nome_chave=?,buffer=?,versao=versao+1 WHERE empresa_id=? AND id=?",nome,nome.toLowerCase(Locale.ROOT),buffer,empresa,id);
        jdbc.update("DELETE FROM workflow_etapa WHERE workflow_id=?",id);jdbc.update("DELETE FROM workflow_gatilho WHERE workflow_id=?",id);
        for(int i=0;i<etapas.size();i++){var etapa=etapas.get(i);jdbc.update("INSERT INTO workflow_etapa(workflow_id,id,nome,responsavel,dias,ordem) VALUES (?,?,?,?,?,?)",id,etapa.id(),etapa.nome(),etapa.responsavel(),etapa.dias(),i);}
        for(var gatilho:gatilhos) jdbc.update("INSERT INTO workflow_gatilho(empresa_id,workflow_id,tarefa_id) VALUES (?,?,?)",empresa,id,gatilho);
        return new Workflow(id,nome,buffer,versao+1,List.copyOf(gatilhos),List.copyOf(etapas));
    }
    @Transactional public void excluir(UUID usuario,UUID id,int versao) {
        UUID empresa=editar(usuario);
        var atual=jdbc.query("SELECT versao FROM escritorio_workflow WHERE empresa_id=? AND id=?",(rs,n)->rs.getInt(1),empresa,id).stream().findFirst().orElseThrow(()->new RecursoNaoEncontradoException("Workflow não encontrado na sua empresa."));
        if(atual!=versao) throw new ConflitoException("Este workflow foi atualizado em outra sessão. Reabra a tela antes de excluir.");
        jdbc.update("DELETE FROM escritorio_workflow WHERE empresa_id=? AND id=?",empresa,id);
    }
}

