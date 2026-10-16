package com.advocacia_microservice.crm.infrastructure;

import com.advocacia_microservice.crm.domain.Lead;
import com.advocacia_microservice.shared.exception.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class CrmRepository {
    private final JdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();
    public CrmRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public String json(Object value) { return json.writeValueAsString(value); }
    public <T> T ler(String value,Class<T> tipo) { return json.readValue(value,tipo); }
    public void bloquear(UUID empresa) { jdbc.queryForObject("SELECT id FROM empresa WHERE id=? FOR UPDATE",UUID.class,empresa); }
    public List<Lead.Passo> cadencia(UUID empresa) {
        var passos=jdbc.query("SELECT passo,dias,rotulo FROM crm_cadencia WHERE empresa_id=? ORDER BY passo",(r,n)->new Lead.Passo(r.getInt(1),r.getInt(2),r.getString(3)),empresa);
        return passos.isEmpty()?List.of(new Lead.Passo(0,0,"Primeiro contato"),new Lead.Passo(1,2,"Primeiro retorno"),new Lead.Passo(2,3,"Segundo retorno"),new Lead.Passo(3,7,"Último retorno")):passos;
    }
    public Lead buscar(UUID empresa,UUID id) {
        var dados=carregar(empresa,List.of(id));
        if(dados.isEmpty()) throw new RecursoNaoEncontradoException("Lead não encontrado."); return dados.getFirst();
    }
    public List<Lead> carregar(UUID empresa,List<UUID> ids) {
        if(ids.isEmpty()) return List.of();
        var marcas=String.join(",",Collections.nCopies(ids.size(),"?"));
        var args=new ArrayList<Object>();args.add(empresa);args.addAll(ids);
        var contatos=new HashMap<UUID,List<Lead.Tratativa>>();
        jdbc.query("SELECT * FROM crm_tratativa WHERE empresa_id=? AND lead_id IN ("+marcas+") ORDER BY data DESC,registrado_em DESC,id",r->{
            contatos.computeIfAbsent(r.getObject("lead_id",UUID.class),k->new ArrayList<>()).add(new Lead.Tratativa(r.getObject("id",UUID.class),r.getDate("data").toLocalDate(),r.getString("canal"),r.getString("resultado"),r.getString("observacao"),r.getObject("autor_id",UUID.class)));
        },args.toArray());
        var comentarios=new HashMap<UUID,List<Lead.Comentario>>();
        var mencoes=new HashMap<UUID,List<UUID>>();
        jdbc.query("SELECT m.comentario_id,m.usuario_id FROM crm_comentario_mencao m JOIN crm_comentario c ON c.id=m.comentario_id WHERE c.empresa_id=? AND c.lead_id IN ("+marcas+")",r->{mencoes.computeIfAbsent(r.getObject(1,UUID.class),k->new ArrayList<>()).add(r.getObject(2,UUID.class));},args.toArray());
        jdbc.query("SELECT c.*,u.nome AS autor FROM crm_comentario c JOIN usuario u ON u.id=c.autor_id WHERE c.empresa_id=? AND c.lead_id IN ("+marcas+") ORDER BY c.criado_em,c.id",r->{
            var id=r.getObject("id",UUID.class);
            comentarios.computeIfAbsent(r.getObject("lead_id",UUID.class),k->new ArrayList<>()).add(new Lead.Comentario(id,r.getObject("autor_id",UUID.class),r.getString("autor"),r.getString("texto"),r.getString("criado_em"),mencoes.getOrDefault(id,List.of())));
        },args.toArray());
        var mapa=new HashMap<UUID,Lead>();
        var cadencia=cadencia(empresa);
        jdbc.query("SELECT id,versao,dados FROM crm_lead WHERE empresa_id=? AND id IN ("+marcas+")",r->{
            var id=r.getObject(1,UUID.class);var d=ler(r.getString(3),Lead.Dados.class);
            mapa.put(id,new Lead(id,empresa,r.getInt(2),d,Lead.proximo(d,cadencia),contatos.getOrDefault(id,List.of()),comentarios.getOrDefault(id,List.of())));
        },args.toArray());
        return ids.stream().filter(mapa::containsKey).map(mapa::get).toList();
    }
    public void salvar(UUID empresa,UUID id,int versao,Lead.Dados d,boolean novo) {
        var c=d.cadastro(); var proximo=Lead.proximo(d,cadencia(empresa));
        var busca=String.join(" ",c.nome(),c.telefone(),c.email(),c.necessidade(),c.observacoes()).toLowerCase(Locale.ROOT);
        Object[] valores={versao,c.nome(),d.estado().status(),c.temperatura(),c.origem(),busca,d.entrada(),proximo,json(d),empresa,id};
        if(novo) jdbc.update("INSERT INTO crm_lead(versao,nome,status,temperatura,origem,busca,entrada,proximo,dados,empresa_id,id) VALUES (?,?,?,?,?,?,?,?,?,?,?)",valores);
        else jdbc.update("UPDATE crm_lead SET versao=?,nome=?,status=?,temperatura=?,origem=?,busca=?,entrada=?,proximo=?,dados=? WHERE empresa_id=? AND id=?",valores);
        jdbc.update("DELETE FROM crm_etiqueta WHERE empresa_id=? AND lead_id=?",empresa,id);
        for(var etiqueta:c.etiquetas()) jdbc.update("INSERT INTO crm_etiqueta(empresa_id,lead_id,etiqueta_id) VALUES (?,?,?)",empresa,id,etiqueta);
    }
    public record Filtro(String sql,List<Object> args) {}
    public Filtro filtro(UUID empresa,String busca,String status,String temperatura,String origem,boolean atrasados) {
        var sql=new StringBuilder("empresa_id=?"); var args=new ArrayList<Object>();args.add(empresa);
        if(!busca.isBlank()){sql.append(" AND busca LIKE ? ESCAPE '!'");args.add("%"+busca.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");}
        if(!status.equals("todos")){sql.append(" AND status=?");args.add(status);}
        if(!temperatura.isEmpty()){sql.append(" AND temperatura=?");args.add(temperatura);}
        if(!origem.isEmpty()){sql.append(" AND origem=?");args.add(origem);}
        if(atrasados){sql.append(" AND status='ativo' AND proximo<=?");args.add(java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")));}
        return new Filtro(sql.toString(),args);
    }
    public long total(Filtro f) { return jdbc.queryForObject("SELECT COUNT(*) FROM crm_lead WHERE "+f.sql,Long.class,f.args.toArray()); }
    public List<UUID> ids(Filtro f,int pagina,int tamanho) {
        var args=new ArrayList<>(f.args);args.add(tamanho);args.add(pagina*tamanho);
        return jdbc.queryForList("SELECT id FROM crm_lead WHERE "+f.sql+" ORDER BY CASE WHEN status='ativo' THEN 0 ELSE 1 END,proximo ASC NULLS LAST,entrada DESC,id LIMIT ? OFFSET ?",UUID.class,args.toArray());
    }
}
