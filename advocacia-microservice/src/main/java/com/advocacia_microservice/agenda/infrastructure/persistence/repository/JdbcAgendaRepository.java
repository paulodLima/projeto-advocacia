package com.advocacia_microservice.agenda.infrastructure.persistence.repository;

import com.advocacia_microservice.agenda.domain.*;
import com.advocacia_microservice.agenda.domain.repository.AgendaRepository;
import com.advocacia_microservice.shared.exception.ConflitoException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAgendaRepository implements AgendaRepository {
    private final JdbcTemplate jdbc;
    public JdbcAgendaRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public void bloquear(UUID empresa) { jdbc.queryForObject("SELECT id FROM empresa WHERE id=? FOR UPDATE",UUID.class,empresa); }
    public Optional<AtividadeAgenda> buscar(UUID empresa,UUID id) {
        var itens=jdbc.query("SELECT * FROM agenda_atividade WHERE empresa_id=? AND id=?",this::atividade,empresa,id);
        if (itens.isEmpty()) return Optional.empty();
        var a=comResponsaveis(itens).getFirst();
        var eventos=jdbc.query("SELECT * FROM agenda_evento WHERE empresa_id=? AND atividade_id=? ORDER BY criado_em DESC,id DESC",
            (r,n)->new EventoAgenda(r.getObject("id",UUID.class),r.getString("autor_nome"),r.getString("acao"),r.getString("observacao"),r.getTimestamp("criado_em").toInstant()),empresa,id);
        return Optional.of(new AtividadeAgenda(a.id(),empresa,a.versao(),a.dados(),a.situacao(),a.concluidaEm(),a.criadoEm(),a.atualizadoEm(),eventos));
    }
    private AtividadeAgenda atividade(ResultSet r,int n) throws SQLException {
        var dados=new DadosAtividade(TipoAtividade.valueOf(r.getString("tipo")),r.getString("titulo"),r.getObject("tipo_tarefa_id",UUID.class),
            r.getObject("data",LocalDate.class),r.getObject("data_fim",LocalDate.class),r.getObject("hora",LocalTime.class),r.getObject("hora_fim",LocalTime.class),r.getObject("prazo_fatal",LocalDate.class),r.getBoolean("dia_inteiro"),r.getString("modalidade"),r.getString("local"),r.getString("descricao"),List.of(),List.of(),RepeticaoAgenda.valueOf(r.getString("repeticao")));
        return new AtividadeAgenda(r.getObject("id",UUID.class),r.getObject("empresa_id",UUID.class),r.getLong("versao"),dados,SituacaoAgenda.valueOf(r.getString("situacao")),
            r.getTimestamp("concluida_em")==null?null:r.getTimestamp("concluida_em").toInstant(),r.getTimestamp("criado_em").toInstant(),r.getTimestamp("atualizado_em").toInstant(),List.of());
    }
    private List<AtividadeAgenda> comResponsaveis(List<AtividadeAgenda> itens) {
        if (itens.isEmpty()) return itens;
        var params=new ArrayList<Object>();params.add(itens.getFirst().empresaId());itens.forEach(a->params.add(a.id()));
        var ids=String.join(",",Collections.nCopies(itens.size(),"?"));var responsaveis=new HashMap<UUID,List<UUID>>();var participantes=new HashMap<UUID,List<UUID>>();
        jdbc.query("SELECT atividade_id,usuario_id FROM agenda_responsavel WHERE empresa_id=? AND atividade_id IN ("+ids+") ORDER BY usuario_id",
            r->{responsaveis.computeIfAbsent(r.getObject(1,UUID.class),k->new ArrayList<>()).add(r.getObject(2,UUID.class));},params.toArray());
        jdbc.query("SELECT atividade_id,contato_id FROM agenda_participante WHERE empresa_id=? AND atividade_id IN ("+ids+") ORDER BY contato_id",
            r->{participantes.computeIfAbsent(r.getObject(1,UUID.class),k->new ArrayList<>()).add(r.getObject(2,UUID.class));},params.toArray());
        return itens.stream().map(a->{var d=a.dados();return new AtividadeAgenda(a.id(),a.empresaId(),a.versao(),
            new DadosAtividade(d.tipo(),d.titulo(),d.tipoTarefaId(),d.data(),d.dataFim(),d.hora(),d.horaFim(),d.prazoFatal(),d.diaInteiro(),d.modalidade(),d.local(),d.descricao(),responsaveis.getOrDefault(a.id(),List.of()),participantes.getOrDefault(a.id(),List.of()),d.repeticao()),a.situacao(),a.concluidaEm(),a.criadoEm(),a.atualizadoEm(),a.historico());}).toList();
    }
    private String filtrar(UUID empresa,FiltroAgenda f,List<Object> params) {
        params.add(empresa);params.add(f.fim());params.add(f.inicio());params.add(f.inicio());params.add(f.fim());
        var sql=new StringBuilder(" WHERE a.empresa_id=? AND ((a.data<=? AND COALESCE(a.data_fim,a.data)>=?) OR a.prazo_fatal BETWEEN ? AND ?");
        if (f.incluirSemData()) sql.append(" OR a.data IS NULL");
        sql.append(")");
        if (f.tipo()!=null) {sql.append(" AND a.tipo=?");params.add(f.tipo().name());}
        if (f.situacao()!=null) {sql.append(" AND a.situacao=?");params.add(f.situacao().name());}
        if (f.responsavel()!=null) {sql.append(" AND EXISTS(SELECT 1 FROM agenda_responsavel ar WHERE ar.atividade_id=a.id AND ar.empresa_id=a.empresa_id AND ar.usuario_id=?)");params.add(f.responsavel());}
        if (!f.busca().isBlank()) {sql.append(" AND LOWER(a.titulo) LIKE ? ESCAPE '!'");params.add("%"+f.busca().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");}
        return sql.toString();
    }
    public PaginaAgenda listar(UUID empresa,FiltroAgenda filtro,int pagina,int tamanho) {
        var params=new ArrayList<Object>();var where=filtrar(empresa,filtro,params);
        var total=jdbc.queryForObject("SELECT COUNT(*) FROM agenda_atividade a"+where,Long.class,params.toArray());
        params.add(tamanho);params.add((long)pagina*tamanho);
        var itens=jdbc.query("SELECT a.* FROM agenda_atividade a"+where+" ORDER BY CASE WHEN a.data IS NULL THEN 1 ELSE 0 END,a.data,a.hora,a.id LIMIT ? OFFSET ?",this::atividade,params.toArray());
        return new PaginaAgenda(comResponsaveis(itens),total,pagina,tamanho);
    }
    public List<MarcoAgenda> marcos(UUID empresa,FiltroAgenda filtro) {
        var params=new ArrayList<Object>();var where=filtrar(empresa,filtro,params);
        return jdbc.query("SELECT a.data,a.data_fim,a.prazo_fatal,a.situacao FROM agenda_atividade a"+where,
            (r,n)->new MarcoAgenda(r.getObject(1,LocalDate.class),r.getObject(2,LocalDate.class),r.getObject(3,LocalDate.class),SituacaoAgenda.valueOf(r.getString(4))),params.toArray());
    }
    public List<OpcaoAgenda> responsaveis(UUID empresa) {
        return jdbc.query("SELECT u.id,CASE WHEN m.nome_exibicao IS NOT NULL AND m.nome_exibicao<>'' THEN m.nome_exibicao ELSE u.nome END nome, CASE WHEN u.status='ATIVO' AND COALESCE(m.situacao,'ATIVO')='ATIVO' THEN TRUE ELSE FALSE END ativo FROM empresa_usuario v JOIN usuario u ON u.id=v.usuario_id LEFT JOIN equipe_membro m ON m.usuario_id=u.id WHERE v.empresa_id=? ORDER BY u.nome,u.id",
            (r,n)->new OpcaoAgenda(r.getObject(1,UUID.class),r.getString(2),r.getBoolean(3)),empresa);
    }
    public List<OpcaoAgenda> tiposTarefa(UUID empresa) {
        return jdbc.query("SELECT id,nome,ativo FROM cadastro_item WHERE empresa_id=? AND tipo='tarefas' ORDER BY nome_chave,id",(r,n)->new OpcaoAgenda(r.getObject(1,UUID.class),r.getString(2),r.getBoolean(3)),empresa);
    }
    public List<OpcaoAgenda> contatos(UUID empresa,String busca,List<UUID> ids) {
        var args=new ArrayList<Object>();args.add(empresa);
        var sql="SELECT id,nome FROM contato WHERE empresa_id=?";
        if (!ids.isEmpty()) {
            sql+=" AND id IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+")";args.addAll(ids);
        } else {
            sql+=" AND LOWER(nome) LIKE ? ESCAPE '!'";
            args.add("%"+busca.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");
        }
        return jdbc.query(sql+" ORDER BY LOWER(nome),id LIMIT 50",(r,n)->new OpcaoAgenda(r.getObject(1,UUID.class),r.getString(2),true),args.toArray());
    }
    public CicloRecorrencia ciclo(UUID empresa,UUID atividade) {
        return jdbc.queryForObject("SELECT COALESCE(repeticao_base,data),repeticao_indice FROM agenda_atividade WHERE empresa_id=? AND id=?",
            (r,n)->new CicloRecorrencia(r.getObject(1,LocalDate.class),r.getInt(2)),empresa,atividade);
    }
    public boolean temSucessora(UUID empresa,UUID atividade) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM agenda_atividade WHERE empresa_id=? AND anterior_id=?",Long.class,empresa,atividade)>0;
    }
    public void vincularSucessora(UUID empresa,UUID anterior,UUID proxima,CicloRecorrencia ciclo) {
        jdbc.update("UPDATE agenda_atividade SET anterior_id=?,repeticao_base=?,repeticao_indice=? WHERE empresa_id=? AND id=?",anterior,ciclo.base(),ciclo.indice()+1,empresa,proxima);
    }
    public void salvar(AtividadeAgenda a,boolean nova) {
        var d=a.dados();
        var params=new ArrayList<Object>(Arrays.asList(a.versao(),d.tipo().name(),d.titulo(),d.tipoTarefaId(),d.data(),d.dataFim(),d.hora(),d.horaFim(),d.prazoFatal(),d.diaInteiro(),d.modalidade(),d.local(),d.descricao(),a.situacao().name(),a.concluidaEm()==null?null:java.sql.Timestamp.from(a.concluidaEm()),java.sql.Timestamp.from(a.criadoEm()),java.sql.Timestamp.from(a.atualizadoEm()),a.empresaId(),a.id()));
        if (nova) {
            params.addAll(17,Arrays.asList(d.repeticao().name(),d.data(),d.data()));
            jdbc.update("INSERT INTO agenda_atividade(versao,tipo,titulo,tipo_tarefa_id,data,data_fim,hora,hora_fim,prazo_fatal,dia_inteiro,modalidade,local,descricao,situacao,concluida_em,criado_em,atualizado_em,repeticao,repeticao_base,repeticao_data,empresa_id,id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",params.toArray());
        }
        else {
            params.addAll(17,Arrays.asList(d.repeticao().name(),d.data(),d.data(),d.repeticao().name(),d.data(),d.data(),d.repeticao().name()));
            params.add(a.versao()-1);
            var alterados=jdbc.update("UPDATE agenda_atividade SET versao=?,tipo=?,titulo=?,tipo_tarefa_id=?,data=?,data_fim=?,hora=?,hora_fim=?,prazo_fatal=?,dia_inteiro=?,modalidade=?,local=?,descricao=?,situacao=?,concluida_em=?,criado_em=?,atualizado_em=?,repeticao_base=CASE WHEN repeticao<>? OR repeticao_data IS DISTINCT FROM ? THEN ? ELSE repeticao_base END,repeticao_indice=CASE WHEN repeticao<>? OR repeticao_data IS DISTINCT FROM ? THEN 0 ELSE repeticao_indice END,repeticao_data=?,repeticao=? WHERE empresa_id=? AND id=? AND versao=?",params.toArray());
            if (alterados!=1) throw new ConflitoException("A atividade foi alterada. Recarregue a ficha.");
        }
        jdbc.update("DELETE FROM agenda_responsavel WHERE empresa_id=? AND atividade_id=?",a.empresaId(),a.id());
        for (var id:d.responsaveis()) jdbc.update("INSERT INTO agenda_responsavel(empresa_id,atividade_id,usuario_id) VALUES(?,?,?)",a.empresaId(),a.id(),id);
        jdbc.update("DELETE FROM agenda_participante WHERE empresa_id=? AND atividade_id=?",a.empresaId(),a.id());
        for (var id:d.participantes()) jdbc.update("INSERT INTO agenda_participante(empresa_id,atividade_id,contato_id) VALUES(?,?,?)",a.empresaId(),a.id(),id);
    }
    public void registrar(UUID empresa,UUID atividade,UUID autor,String acao,String observacao) {
        var nome=jdbc.queryForObject("SELECT nome FROM usuario WHERE id=?",String.class,autor);
        jdbc.update("INSERT INTO agenda_evento(id,empresa_id,atividade_id,autor_id,autor_nome,acao,observacao) VALUES(?,?,?,?,?,?,?)",UUID.randomUUID(),empresa,atividade,autor,nome,acao,observacao);
    }
}
