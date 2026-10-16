package com.advocacia_microservice.agenda.infrastructure.persistence.repository;

import com.advocacia_microservice.agenda.domain.*;
import com.advocacia_microservice.agenda.domain.repository.LembretesAgendaRepository;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLembretesAgendaRepository implements LembretesAgendaRepository {
    private final JdbcTemplate jdbc;
    public JdbcLembretesAgendaRepository(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public void bloquear(UUID empresa) {jdbc.queryForObject("SELECT id FROM empresa WHERE id=? FOR UPDATE",UUID.class,empresa);}
    public PreferenciaLembrete preferencia(UUID empresa,UUID usuario) {
        return jdbc.query("SELECT ativo,antecedencia_minutos FROM agenda_lembrete_preferencia WHERE empresa_id=? AND usuario_id=?",
            (r,n)->new PreferenciaLembrete(r.getBoolean(1),r.getInt(2)),empresa,usuario).stream().findFirst().orElse(PreferenciaLembrete.padrao());
    }
    public void salvarPreferencia(UUID empresa,UUID usuario,PreferenciaLembrete p) {
        if(jdbc.update("UPDATE agenda_lembrete_preferencia SET ativo=?,antecedencia_minutos=? WHERE empresa_id=? AND usuario_id=?",p.ativo(),p.antecedenciaMinutos(),empresa,usuario)==0)
            jdbc.update("INSERT INTO agenda_lembrete_preferencia(empresa_id,usuario_id,ativo,antecedencia_minutos) VALUES(?,?,?,?)",empresa,usuario,p.ativo(),p.antecedenciaMinutos());
    }
    private String base(UUID empresa,UUID usuario,LocalDateTime limite,List<Object> args,boolean naoLidos) {
        args.add(empresa);args.add(usuario);args.add(limite.toLocalDate());args.add(limite.toLocalDate());args.add(limite.toLocalTime());
        String sql="""
            WITH minhas AS (
                SELECT a.* FROM agenda_atividade a
                JOIN agenda_responsavel ar ON ar.empresa_id=a.empresa_id AND ar.atividade_id=a.id
                WHERE a.empresa_id=? AND ar.usuario_id=? AND a.situacao='PENDENTE'
            ), lembretes AS (
                SELECT id,versao,titulo,tipo,data,COALESCE(hora,TIME '09:00:00') AS hora,'ATIVIDADE' AS marco FROM minhas
                WHERE data IS NOT NULL AND (prazo_fatal IS NULL OR data<>prazo_fatal OR COALESCE(hora,TIME '09:00:00')<>TIME '09:00:00')
                UNION ALL
                SELECT id,versao,titulo,tipo,prazo_fatal AS data,TIME '09:00:00' AS hora,'PRAZO_FATAL' AS marco FROM minhas WHERE prazo_fatal IS NOT NULL
            )
            SELECT l.* FROM lembretes l WHERE (l.data<? OR (l.data=? AND l.hora<=?))
            """;
        if(naoLidos) {
            sql+=" AND NOT EXISTS(SELECT 1 FROM agenda_lembrete_lido v WHERE v.empresa_id=? AND v.usuario_id=? AND v.atividade_id=l.id AND v.versao=l.versao AND v.marco=l.marco)";
            args.add(empresa);args.add(usuario);
        }
        return sql;
    }
    private LembreteAgenda ler(java.sql.ResultSet r,int n) throws java.sql.SQLException {
        return new LembreteAgenda(r.getObject("id",UUID.class),r.getLong("versao"),r.getString("titulo"),TipoAtividade.valueOf(r.getString("tipo")),MarcoLembrete.valueOf(r.getString("marco")),r.getObject("data",LocalDate.class),r.getObject("hora",LocalTime.class));
    }
    public PaginaLembretes listar(UUID empresa,UUID usuario,LocalDateTime limite,int pagina,int tamanho) {
        var args=new ArrayList<Object>();var sql=base(empresa,usuario,limite,args,true);
        var countSql=sql.replace("SELECT l.* FROM lembretes l WHERE","SELECT COUNT(*) FROM lembretes l WHERE");
        long total=jdbc.queryForObject(countSql,Long.class,args.toArray());
        args.add(tamanho);args.add((long)pagina*tamanho);
        var itens=jdbc.query(sql+" ORDER BY l.data,l.hora,CASE WHEN l.marco='PRAZO_FATAL' THEN 0 ELSE 1 END,l.id LIMIT ? OFFSET ?",this::ler,args.toArray());
        return new PaginaLembretes(itens,total,pagina,tamanho);
    }
    public Optional<LembreteAgenda> buscar(UUID empresa,UUID usuario,LocalDateTime limite,UUID atividade,long versao,MarcoLembrete marco) {
        var args=new ArrayList<Object>();var sql=base(empresa,usuario,limite,args,false);
        args.add(atividade);args.add(versao);args.add(marco.name());
        return jdbc.query(sql+" AND l.id=? AND l.versao=? AND l.marco=?",this::ler,args.toArray()).stream().findFirst();
    }
    public void ler(UUID empresa,UUID usuario,LembreteAgenda l) {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM agenda_lembrete_lido WHERE empresa_id=? AND usuario_id=? AND atividade_id=? AND versao=? AND marco=?",Long.class,empresa,usuario,l.atividadeId(),l.versao(),l.marco().name())==0)
            jdbc.update("INSERT INTO agenda_lembrete_lido(empresa_id,usuario_id,atividade_id,versao,marco) VALUES(?,?,?,?,?)",empresa,usuario,l.atividadeId(),l.versao(),l.marco().name());
    }
}
