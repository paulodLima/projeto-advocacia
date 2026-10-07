package com.advocacia_microservice.cadastro.infrastructure;

import com.advocacia_microservice.cadastro.domain.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CadastrosRepository {
    public record Rotina(UUID id, String nome, String periodo) {}
    private final JdbcTemplate jdbc;
    public CadastrosRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void bloquear(UUID empresa) { jdbc.queryForObject("SELECT id FROM empresa WHERE id = ? FOR UPDATE", UUID.class, empresa); }
    public List<CadastroRegistro> listar(UUID empresa) {
        var campos = new LinkedHashMap<UUID, Map<String, String>>();
        var tipos = new HashMap<UUID, String>(); var ativos = new HashMap<UUID, Boolean>();
        jdbc.query("SELECT i.id, i.tipo, i.ativo, c.campo, c.valor FROM cadastro_item i JOIN cadastro_campo c ON c.item_id = i.id WHERE i.empresa_id = ? ORDER BY i.tipo, i.nome_chave, i.id, c.campo", rs -> {
            UUID id = rs.getObject("id", UUID.class); tipos.put(id, rs.getString("tipo")); ativos.put(id, rs.getBoolean("ativo"));
            campos.computeIfAbsent(id, k -> new HashMap<>()).put(rs.getString("campo"), rs.getString("valor"));
        }, empresa);
        return campos.entrySet().stream().map(e -> new CadastroRegistro(e.getKey(), tipos.get(e.getKey()), ativos.get(e.getKey()), e.getValue(), null)).toList();
    }
    public void salvar(UUID empresa, CadastroRegistro item, boolean novo) {
        if (novo) jdbc.update("INSERT INTO cadastro_item(id,empresa_id,tipo,nome,nome_chave,ativo,referencia_id) VALUES (?,?,?,?,?,?,?)", item.id(), empresa, item.tipo(), item.campos().get("nome"), item.campos().get("nome").toLowerCase(Locale.ROOT), item.ativo(), item.referenciaId());
        else jdbc.update("UPDATE cadastro_item SET nome=?,nome_chave=?,ativo=?,referencia_id=? WHERE empresa_id=? AND id=?", item.campos().get("nome"), item.campos().get("nome").toLowerCase(Locale.ROOT), item.ativo(), item.referenciaId(), empresa, item.id());
        jdbc.update("DELETE FROM cadastro_campo WHERE item_id=?", item.id());
        item.campos().forEach((campo, valor) -> jdbc.update("INSERT INTO cadastro_campo(item_id,campo,valor) VALUES (?,?,?)", item.id(), campo, valor));
    }
    public boolean usado(UUID empresa, UUID id) { return jdbc.queryForObject("SELECT COUNT(*) FROM cadastro_item WHERE empresa_id=? AND referencia_id=?", Long.class, empresa, id) > 0 || jdbc.queryForObject("SELECT COUNT(*) FROM workflow_gatilho WHERE empresa_id=? AND tarefa_id=?",Long.class,empresa,id)>0; }
    public void excluir(UUID empresa, UUID id) { jdbc.update("DELETE FROM cadastro_item WHERE empresa_id=? AND id=?", empresa, id); }
    public List<Rotina> rotinas(UUID empresa) { return jdbc.query("SELECT id,nome,periodo FROM escritorio_rotina WHERE empresa_id=? ORDER BY periodo,nome_chave", (rs, n) -> new Rotina(rs.getObject("id", UUID.class), rs.getString("nome"), rs.getString("periodo")), empresa); }
    public Rotina criarRotina(UUID empresa, String nome, String periodo) {
        var rotina = new Rotina(UUID.randomUUID(), nome, periodo);
        jdbc.update("INSERT INTO escritorio_rotina(id,empresa_id,nome,nome_chave,periodo) VALUES (?,?,?,?,?)", rotina.id(), empresa, nome, nome.toLowerCase(Locale.ROOT), periodo); return rotina;
    }
    public int excluirRotina(UUID empresa, UUID id) { return jdbc.update("DELETE FROM escritorio_rotina WHERE empresa_id=? AND id=?", empresa, id); }
    public List<Sistema> sistemas(UUID empresa) { return jdbc.query("SELECT * FROM escritorio_sistema WHERE empresa_id=? ORDER BY ordem,id", (rs, n) -> new Sistema(rs.getObject("id", UUID.class), rs.getString("nome"), rs.getString("url"), rs.getBoolean("ativo"), rs.getString("icone"), rs.getString("cor"), rs.getString("logo")), empresa); }
    public List<Sistema> sistemasAtivos(UUID empresa) { return jdbc.query("SELECT * FROM escritorio_sistema WHERE empresa_id=? AND ativo=true ORDER BY ordem,id", (rs, n) -> new Sistema(rs.getObject("id", UUID.class), rs.getString("nome"), rs.getString("url"), rs.getBoolean("ativo"), rs.getString("icone"), rs.getString("cor"), rs.getString("logo")), empresa); }
    public UUID empresaDoSistema(UUID id) { return jdbc.query("SELECT empresa_id FROM escritorio_sistema WHERE id=?", (rs, n) -> rs.getObject(1, UUID.class), id).stream().findFirst().orElse(null); }
    public void salvarSistemas(UUID empresa, List<Sistema> sistemas) {
        jdbc.update("DELETE FROM escritorio_sistema WHERE empresa_id=?", empresa);
        for (int i = 0; i < sistemas.size(); i++) {
            var s = sistemas.get(i);
            jdbc.update("INSERT INTO escritorio_sistema(id,empresa_id,nome,url,ativo,icone,cor,logo,ordem) VALUES (?,?,?,?,?,?,?,?,?)", s.id(), empresa, s.nome(), s.url(), s.ativo(), s.icone(), s.cor(), s.logo(), i);
        }
    }
}
