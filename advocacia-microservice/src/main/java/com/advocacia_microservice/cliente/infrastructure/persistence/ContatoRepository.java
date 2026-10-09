package com.advocacia_microservice.cliente.infrastructure.persistence;

import com.advocacia_microservice.cliente.domain.Contato;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.advocacia_microservice.shared.exception.*;

@Repository
public class ContatoRepository {
    private final JdbcTemplate jdbc;
    public ContatoRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public void bloquear(UUID empresa) { jdbc.queryForObject("SELECT id FROM empresa WHERE id=? FOR UPDATE",UUID.class,empresa); }
    public Contato buscar(UUID empresa,UUID id) {
        var versoes=jdbc.queryForList("SELECT versao FROM contato WHERE empresa_id=? AND id=?",Integer.class,empresa,id);
        if(versoes.isEmpty()) throw new RecursoNaoEncontradoException("Contato não encontrado.");
        var dados=new HashMap<String,String>(); jdbc.query("SELECT campo,valor FROM contato_dado WHERE contato_id=?",rs->{dados.put(rs.getString(1),rs.getString(2));},id);
        var reps=new TreeMap<Integer,Map<String,String>>();jdbc.query("SELECT ordem,campo,valor FROM contato_representante WHERE contato_id=?",rs->{reps.computeIfAbsent(rs.getInt(1),k->new HashMap<>()).put(rs.getString(2),rs.getString(3));},id);
        var tags=jdbc.queryForList("SELECT indicador_id FROM contato_tag WHERE empresa_id=? AND contato_id=? ORDER BY indicador_id",UUID.class,empresa,id);
        var incompleto=jdbc.queryForObject("SELECT cadastro_incompleto FROM contato WHERE empresa_id=? AND id=?",Boolean.class,empresa,id);
        return new Contato(id,empresa,versoes.getFirst(),dados,new ArrayList<>(reps.values()),tags,incompleto);
    }
    // Carrega a página em lotes: a quantidade de consultas não cresce com os contatos.
    public List<Contato> carregarPagina(UUID empresa,List<UUID> ids) {
        if(ids.isEmpty()) return List.of();
        String marcas=String.join(",",Collections.nCopies(ids.size(),"?"));
        var parametros=new ArrayList<Object>();parametros.add(empresa);parametros.addAll(ids);
        String filtro=" IN (SELECT id FROM contato WHERE empresa_id=? AND id IN ("+marcas+"))";
        var dados=new HashMap<UUID,Map<String,String>>();var reps=new HashMap<UUID,TreeMap<Integer,Map<String,String>>>();var tags=new HashMap<UUID,List<UUID>>();var versoes=new HashMap<UUID,Integer>();
        jdbc.query("SELECT id,versao FROM contato WHERE empresa_id=? AND id IN ("+marcas+")",rs->{versoes.put(rs.getObject(1,UUID.class),rs.getInt(2));},parametros.toArray());
        jdbc.query("SELECT contato_id,campo,valor FROM contato_dado WHERE contato_id"+filtro,rs->{dados.computeIfAbsent(rs.getObject(1,UUID.class),k->new HashMap<>()).put(rs.getString(2),rs.getString(3));},parametros.toArray());
        jdbc.query("SELECT contato_id,ordem,campo,valor FROM contato_representante WHERE contato_id"+filtro,rs->{reps.computeIfAbsent(rs.getObject(1,UUID.class),k->new TreeMap<>()).computeIfAbsent(rs.getInt(2),k->new HashMap<>()).put(rs.getString(3),rs.getString(4));},parametros.toArray());
        jdbc.query("SELECT contato_id,indicador_id FROM contato_tag WHERE contato_id"+filtro+" ORDER BY indicador_id",rs->{tags.computeIfAbsent(rs.getObject(1,UUID.class),k->new ArrayList<>()).add(rs.getObject(2,UUID.class));},parametros.toArray());
        var incompletos=new HashSet<>(jdbc.queryForList("SELECT id FROM contato WHERE empresa_id=? AND cadastro_incompleto=true AND id IN ("+marcas+")",UUID.class,parametros.toArray()));
        return ids.stream().filter(versoes::containsKey).map(id->new Contato(id,empresa,versoes.get(id),dados.get(id),new ArrayList<>(reps.getOrDefault(id,new TreeMap<>()).values()),tags.getOrDefault(id,List.of()),incompletos.contains(id))).toList();
    }
    public void salvar(Contato c,boolean novo) {
        var d=c.dados(); var doc=d.get("documento").replaceAll("\\D","");
        var iguais=jdbc.queryForList("SELECT id FROM contato WHERE empresa_id=? AND documento_chave=? AND id<>?",UUID.class,c.empresaId(),doc,c.id());
        if(!doc.isEmpty()&&!iguais.isEmpty()) throw new ConflitoException("Já existe um contato com esse CPF/CNPJ. Abra e edite o cadastro existente.");
        Object[] valores={d.get("nome"),d.get("tipo"),d.get("tipo_pessoa"),doc.isEmpty()?null:doc,Contato.uuid(d.get("origem_id")),Contato.uuid(d.get("carteira_parceiro_id")),c.versao(),c.empresaId(),c.id()};
        if(novo) jdbc.update("INSERT INTO contato(nome,tipo,tipo_pessoa,documento_chave,origem_id,parceiro_id,versao,empresa_id,id) VALUES (?,?,?,?,?,?,?,?,?)",valores);
        else {
            jdbc.update("UPDATE contato SET nome=?,tipo=?,tipo_pessoa=?,documento_chave=?,origem_id=?,parceiro_id=?,versao=?,atualizado_em=CURRENT_TIMESTAMP WHERE empresa_id=? AND id=?",valores);
            jdbc.update("DELETE FROM contato_dado WHERE contato_id=?",c.id());jdbc.update("DELETE FROM contato_representante WHERE contato_id=?",c.id());jdbc.update("DELETE FROM contato_tag WHERE contato_id=?",c.id());
        }
        jdbc.update("UPDATE contato SET cadastro_incompleto=? WHERE empresa_id=? AND id=?",c.cadastroIncompleto(),c.empresaId(),c.id());
        d.forEach((k,v)->jdbc.update("INSERT INTO contato_dado(contato_id,campo,valor) VALUES (?,?,?)",c.id(),k,v));
        for(int n=0;n<c.representantes().size();n++) {int ordem=n;c.representantes().get(n).forEach((k,v)->jdbc.update("INSERT INTO contato_representante(contato_id,ordem,campo,valor) VALUES (?,?,?,?)",c.id(),ordem,k,v));}
        c.indicadores().forEach(tag->jdbc.update("INSERT INTO contato_tag(empresa_id,contato_id,indicador_id) VALUES (?,?,?)",c.empresaId(),c.id(),tag));
    }
    public boolean referencia(UUID empresa,UUID id,String tabela,String tipo) {
        if(tabela.equals("cadastro_item")) return jdbc.queryForObject("SELECT COUNT(*) FROM cadastro_item WHERE empresa_id=? AND id=? AND tipo=? AND ativo=true",Long.class,empresa,id,tipo)>0;
        return jdbc.queryForObject("SELECT COUNT(*) FROM contato WHERE empresa_id=? AND id=? AND tipo='Parceiro'",Long.class,empresa,id)>0;
    }
    public List<Map<String,Object>> opcoes(UUID empresa,String tipo) {
        if(tipo.equals("origens")) return jdbc.queryForList("SELECT id,nome FROM cadastro_item WHERE empresa_id=? AND tipo='origens' AND ativo=true ORDER BY nome",empresa);
        return jdbc.queryForList("SELECT id,nome FROM contato WHERE empresa_id=? AND tipo='Parceiro' ORDER BY nome",empresa);
    }
    public List<UUID> ids(UUID empresa,String busca,String tipo,UUID tag,int pagina,int tamanho) {
        var filtro=filtro(empresa,busca,tipo,tag);var args=new ArrayList<>(filtro.args());args.add(tamanho);args.add(pagina*tamanho);
        return jdbc.queryForList("SELECT c.id FROM contato c WHERE "+filtro.sql()+" ORDER BY LOWER(c.nome),c.id LIMIT ? OFFSET ?",UUID.class,args.toArray());
    }
    public long total(UUID empresa,String busca,String tipo,UUID tag) {var f=filtro(empresa,busca,tipo,tag);return jdbc.queryForObject("SELECT COUNT(*) FROM contato c WHERE "+f.sql(),Long.class,f.args().toArray());}
    private record Filtro(String sql,List<Object> args) {}
    private Filtro filtro(UUID empresa,String busca,String tipo,UUID tag) {
        var sql=new StringBuilder("c.empresa_id=?");var args=new ArrayList<Object>();args.add(empresa);
        if(!tipo.isEmpty()){sql.append(" AND c.tipo=?");args.add(tipo);}
        if(tag!=null){sql.append(" AND EXISTS (SELECT 1 FROM contato_tag t WHERE t.contato_id=c.id AND t.indicador_id=?)");args.add(tag);}
        if(!busca.isEmpty()){sql.append(" AND EXISTS (SELECT 1 FROM contato_dado d WHERE d.contato_id=c.id AND d.campo IN ('nome','documento','email','telefone','observacoes') AND LOWER(d.valor) LIKE ? ESCAPE '!')");args.add("%"+busca.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");}
        return new Filtro(sql.toString(),args);
    }
    public void excluir(UUID empresa,UUID id) {jdbc.update("DELETE FROM contato WHERE empresa_id=? AND id=?",empresa,id);}
    public void auditar(UUID empresa,UUID contato,UUID autor,String acao) {jdbc.update("INSERT INTO contato_evento(id,empresa_id,contato_id,autor_id,acao) VALUES (?,?,?,?,?)",UUID.randomUUID(),empresa,contato,autor,acao);}
}
