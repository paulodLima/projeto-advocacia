package com.advocacia_microservice.agenda.infrastructure.persistence.repository;
import com.advocacia_microservice.agenda.domain.*;
import com.advocacia_microservice.agenda.domain.repository.VinculosAgendaRepository;
import com.advocacia_microservice.crm.infrastructure.CrmRepository;
import com.advocacia_microservice.caso.application.TarefasCasoService.Dados;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcVinculosAgendaRepository implements VinculosAgendaRepository {
 private final JdbcTemplate jdbc; private final CrmRepository json; private final org.springframework.transaction.PlatformTransactionManager transactionManager;
 public JdbcVinculosAgendaRepository(JdbcTemplate jdbc,CrmRepository json,org.springframework.transaction.PlatformTransactionManager transactionManager){this.jdbc=jdbc;this.json=json;this.transactionManager=transactionManager;}
 private void sincronizar(ProjecaoAgenda c){
  var args=new ArrayList<Object>(Arrays.asList(c.titulo(),c.lead()!=null?"REUNIAO":"TAREFA",c.data(),c.dataFim(),c.hora(),c.horaFim(),c.prazoFatal(),c.diaInteiro(),c.local(),c.situacao(),c.mostrar(),c.privada(),c.autor(),c.empresa(),c.id()));
  if(jdbc.update("UPDATE agenda_vinculo SET titulo=?,tipo=?,data=?,data_fim=?,hora=?,hora_fim=?,prazo_fatal=?,dia_inteiro=?,local=?,situacao=?,mostrar=?,privada=?,autor_id=? WHERE empresa_id=? AND id=?",args.toArray())==0){
   var insert=new ArrayList<Object>(args.subList(0,15));insert.add(c.lead());insert.add(c.tarefa());insert.add(c.caso());
   jdbc.update("INSERT INTO agenda_vinculo(titulo,tipo,data,data_fim,hora,hora_fim,prazo_fatal,dia_inteiro,local,situacao,mostrar,privada,autor_id,empresa_id,id,lead_id,tarefa_id,caso_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",insert.toArray());
  }
 }

 public void sincronizarConsulta(UUID empresa,UUID lead){
  var consultas=jdbc.query("SELECT * FROM crm_consulta WHERE empresa_id=? AND lead_id=?",(r,n)->new ProjecaoAgenda(empresa,r.getObject("id",UUID.class),lead,null,null,r.getString("titulo"),r.getObject("quando",LocalDateTime.class).toLocalDate(),null,r.getObject("quando",LocalDateTime.class).toLocalTime(),null,null,false,"",switch(r.getString("status")){case "realizada"->"CONCLUIDA";case "nao_compareceu"->"CANCELADA";default->"PENDENTE";},true,false,null),empresa,lead);
  if(consultas.isEmpty())jdbc.update("DELETE FROM agenda_vinculo WHERE empresa_id=? AND lead_id=?",empresa,lead);
  else sincronizar(consultas.getFirst());
 }
 public void sincronizarTarefa(UUID empresa,UUID tarefa){
  var rows=jdbc.queryForList("SELECT * FROM caso_tarefa WHERE empresa_id=? AND id=?",empresa,tarefa);
  if(rows.isEmpty())return;var r=rows.getFirst();var d=json.ler((String)r.get("dados"),Dados.class);
  var titulo=d.titulo();
  if(titulo.isBlank() && d.tipoId()!=null)titulo=jdbc.queryForList("SELECT nome FROM cadastro_item WHERE empresa_id=? AND id=?",String.class,empresa,d.tipoId()).stream().findFirst().orElse("Tarefa");
  sincronizar(new ProjecaoAgenda(empresa,tarefa,null,tarefa,(UUID)r.get("caso_id"),titulo,d.data(),d.dataFim(),d.hora(),d.horaFim(),d.prazoFatal(),d.diaInteiro(),d.local(),r.get("status").equals("concluida")?"CONCLUIDA":"PENDENTE",d.mostrarAgenda(),d.flags().contains("Privada"),(UUID)r.get("autor_id")));
 }
 public void reindexar(){
  // Only missing projections. Source writers take the same office lock and update in their own transaction.
  var empresas=jdbc.queryForList("SELECT id FROM empresa ORDER BY id",UUID.class);
  for(var empresa:empresas){
   var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
   tx.executeWithoutResult(status->{
    jdbc.queryForObject("SELECT id FROM empresa WHERE id=? FOR UPDATE",UUID.class,empresa);
    while(true){
     var leads=jdbc.queryForList("SELECT c.lead_id FROM crm_consulta c WHERE c.empresa_id=? AND NOT EXISTS(SELECT 1 FROM agenda_vinculo v WHERE v.empresa_id=c.empresa_id AND v.lead_id=c.lead_id) ORDER BY c.id LIMIT 100",UUID.class,empresa);
     if(leads.isEmpty())break;leads.forEach(id->sincronizarConsulta(empresa,id));
    }
    while(true){
     var tarefas=jdbc.queryForList("SELECT t.id FROM caso_tarefa t WHERE t.empresa_id=? AND NOT EXISTS(SELECT 1 FROM agenda_vinculo v WHERE v.empresa_id=t.empresa_id AND v.tarefa_id=t.id) ORDER BY t.id LIMIT 100",UUID.class,empresa);
     if(tarefas.isEmpty())break;tarefas.forEach(id->sincronizarTarefa(empresa,id));
    }
   });
  }
 }

 private String filtro(AcessoVinculos a,FiltroAgenda f,List<Object> args){
  args.add(a.empresa());args.add(a.crm());args.add(a.casos());args.add(a.master());args.add(a.usuario());args.add(a.usuario());
  var s=new StringBuilder(" WHERE v.empresa_id=? AND v.mostrar=TRUE AND ((v.lead_id IS NOT NULL AND ?=TRUE) OR (v.tarefa_id IS NOT NULL AND ?=TRUE AND (v.privada=FALSE OR ?=TRUE OR v.autor_id=? OR EXISTS(SELECT 1 FROM caso_tarefa_responsavel r WHERE r.empresa_id=v.empresa_id AND r.tarefa_id=v.tarefa_id AND r.usuario_id=?))))");
  s.append(" AND ((v.data<=? AND COALESCE(v.data_fim,v.data)>=?) OR v.prazo_fatal BETWEEN ? AND ?");
  args.add(f.fim());args.add(f.inicio());args.add(f.inicio());args.add(f.fim());
  if(f.incluirSemData())s.append(" OR v.data IS NULL");s.append(")");
  if(f.tipo()!=null){s.append(" AND v.tipo=?");args.add(f.tipo().name());}
  if(f.situacao()!=null){s.append(" AND v.situacao=?");args.add(f.situacao().name());}
  if(f.responsavel()!=null){s.append(" AND EXISTS(SELECT 1 FROM caso_tarefa_responsavel r WHERE r.empresa_id=v.empresa_id AND r.tarefa_id=v.tarefa_id AND r.usuario_id=?)");args.add(f.responsavel());}
  if(!f.busca().isBlank()){s.append(" AND LOWER(v.titulo) LIKE ? ESCAPE '!'");args.add("%"+f.busca().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");}
  return s.toString();
 }
 private VinculoAgenda mapear(java.sql.ResultSet r,int n)throws java.sql.SQLException{
  return new VinculoAgenda(r.getObject("id",UUID.class),r.getObject("lead_id",UUID.class),r.getObject("tarefa_id",UUID.class),r.getObject("caso_id",UUID.class),r.getString("titulo"),TipoAtividade.valueOf(r.getString("tipo")),r.getObject("data",LocalDate.class),r.getObject("data_fim",LocalDate.class),r.getObject("hora",LocalTime.class),r.getObject("hora_fim",LocalTime.class),r.getObject("prazo_fatal",LocalDate.class),r.getBoolean("dia_inteiro"),r.getString("local"),SituacaoAgenda.valueOf(r.getString("situacao")),List.of());
 }
 public PaginaVinculos listar(AcessoVinculos a,FiltroAgenda f,int pagina,int tamanho){
  var args=new ArrayList<Object>();var where=filtro(a,f,args);
  var total=jdbc.queryForObject("SELECT COUNT(*) FROM agenda_vinculo v"+where,Long.class,args.toArray());
  args.add(tamanho);args.add((long)pagina*tamanho);
  var itens=jdbc.query("SELECT v.* FROM agenda_vinculo v"+where+" ORDER BY CASE WHEN v.data IS NULL THEN 1 ELSE 0 END,v.data,v.hora,v.id LIMIT ? OFFSET ?",this::mapear,args.toArray());
  var pessoas=new HashMap<UUID,List<OpcaoAgenda>>();
  if(!itens.isEmpty()){
   var p=new ArrayList<Object>();p.add(a.empresa());itens.forEach(v->p.add(v.id()));
   jdbc.query("SELECT r.tarefa_id,u.id,u.nome,u.status FROM caso_tarefa_responsavel r JOIN usuario u ON u.id=r.usuario_id WHERE r.empresa_id=? AND r.tarefa_id IN ("+String.join(",",Collections.nCopies(itens.size(),"?"))+") ORDER BY u.nome",r->{pessoas.computeIfAbsent(r.getObject(1,UUID.class),k->new ArrayList<>()).add(new OpcaoAgenda(r.getObject(2,UUID.class),r.getString(3),r.getString(4).equals("ATIVO")));},p.toArray());
  }
  return new PaginaVinculos(itens.stream().map(v->new VinculoAgenda(v.id(),v.leadId(),v.tarefaId(),v.casoId(),v.titulo(),v.tipo(),v.data(),v.dataFim(),v.hora(),v.horaFim(),v.prazoFatal(),v.diaInteiro(),v.local(),v.situacao(),pessoas.getOrDefault(v.id(),List.of()))).toList(),total,pagina,tamanho);
 }
 public List<MarcoAgenda> marcos(AcessoVinculos a,FiltroAgenda f){
  var args=new ArrayList<Object>();var where=filtro(a,f,args);
  return jdbc.query("SELECT v.data,v.data_fim,v.prazo_fatal,v.situacao FROM agenda_vinculo v"+where,(r,n)->new MarcoAgenda(r.getObject(1,LocalDate.class),r.getObject(2,LocalDate.class),r.getObject(3,LocalDate.class),SituacaoAgenda.valueOf(r.getString(4))),args.toArray());
 }
}
