package com.advocacia_microservice.parceiro.infrastructure.persistence;

import com.advocacia_microservice.cliente.domain.Contato;
import com.advocacia_microservice.cliente.infrastructure.persistence.ContatoRepository;
import com.advocacia_microservice.parceiro.domain.Parceiro;
import com.advocacia_microservice.shared.exception.RecursoNaoEncontradoException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ParceiroRepository {
    private final JdbcTemplate jdbc;
    private final ContatoRepository contatos;
    public ParceiroRepository(JdbcTemplate jdbc, ContatoRepository contatos) { this.jdbc = jdbc; this.contatos = contatos; }

    public Parceiro buscar(UUID empresa, UUID id) {
        var contato = contatos.buscar(empresa, id);
        if (!contato.dados().get("tipo").equals("Parceiro")) throw new RecursoNaoEncontradoException("Parceiro não encontrado.");
        return carregar(empresa, List.of(contato)).getFirst();
    }

    public List<Parceiro> carregar(UUID empresa, List<Contato> lista) {
        if (lista.isEmpty()) return List.of();
        var marcas = String.join(",", Collections.nCopies(lista.size(), "?"));
        var args = new ArrayList<Object>(); args.add(empresa); lista.forEach(c -> args.add(c.id()));
        var filtro = " IN (SELECT id FROM contato WHERE empresa_id=? AND tipo='Parceiro' AND id IN (" + marcas + "))";
        var extras = new HashMap<UUID, Map<String, String>>();
        var socios = new HashMap<UUID, List<Parceiro.Socio>>();
        var areas = new HashMap<UUID, List<String>>();
        jdbc.query("SELECT contato_id,campo,valor FROM parceiro_dado WHERE contato_id" + filtro, rs -> {
            extras.computeIfAbsent(rs.getObject(1, UUID.class), k -> new HashMap<>()).put(rs.getString(2), rs.getString(3));
        }, args.toArray());
        jdbc.query("SELECT contato_id,nome,oab FROM parceiro_socio WHERE contato_id" + filtro + " ORDER BY ordem", rs -> {
            socios.computeIfAbsent(rs.getObject(1, UUID.class), k -> new ArrayList<>()).add(new Parceiro.Socio(rs.getString(2), rs.getString(3)));
        }, args.toArray());
        jdbc.query("SELECT contato_id,area FROM parceiro_area WHERE contato_id" + filtro + " ORDER BY area", rs -> {
            areas.computeIfAbsent(rs.getObject(1, UUID.class), k -> new ArrayList<>()).add(rs.getString(2));
        }, args.toArray());
        return lista.stream().map(c -> {
            var dados = new HashMap<String, String>();
            Parceiro.COMUNS.forEach(k -> dados.put(k, c.dados().get(k)));
            dados.putAll(extras.getOrDefault(c.id(), Map.of()));
            return new Parceiro(c.id(), empresa, c.versao(), dados, socios.getOrDefault(c.id(), List.of()), areas.getOrDefault(c.id(), List.of()));
        }).toList();
    }

    public void salvar(Parceiro parceiro) {
        var id = parceiro.id();
        jdbc.update("DELETE FROM parceiro_dado WHERE contato_id=?", id);
        jdbc.update("DELETE FROM parceiro_socio WHERE contato_id=?", id);
        jdbc.update("DELETE FROM parceiro_area WHERE contato_id=?", id);
        Parceiro.EXTRAS.forEach(k -> jdbc.update("INSERT INTO parceiro_dado(contato_id,campo,valor) VALUES (?,?,?)", id, k, parceiro.dados().get(k)));
        for (int n = 0; n < parceiro.socios().size(); n++) {
            var s = parceiro.socios().get(n);
            jdbc.update("INSERT INTO parceiro_socio(contato_id,ordem,nome,oab) VALUES (?,?,?,?)", id, n, s.nome(), s.oab());
        }
        parceiro.areasAtuacao().forEach(a -> jdbc.update("INSERT INTO parceiro_area(contato_id,area) VALUES (?,?)", id, a));
    }

    public record Filtro(String sql, List<Object> args) {}
    public Filtro filtro(UUID empresa, String busca, String uf, String area) {
        var sql = new StringBuilder("c.empresa_id=? AND c.tipo='Parceiro'");
        var args = new ArrayList<Object>(); args.add(empresa);
        if (!uf.isEmpty()) { sql.append(" AND EXISTS (SELECT 1 FROM contato_dado d WHERE d.contato_id=c.id AND d.campo='uf' AND d.valor=?)"); args.add(uf); }
        if (!area.isEmpty()) { sql.append(" AND EXISTS (SELECT 1 FROM parceiro_area a WHERE a.contato_id=c.id AND a.area=?)"); args.add(area); }
        if (!busca.isEmpty()) {
            sql.append(" AND (EXISTS (SELECT 1 FROM contato_dado d WHERE d.contato_id=c.id AND d.campo IN ('nome','nome_fantasia','cidade') AND LOWER(d.valor) LIKE ? ESCAPE '!') OR EXISTS (SELECT 1 FROM parceiro_dado d WHERE d.contato_id=c.id AND d.campo='oab' AND LOWER(d.valor) LIKE ? ESCAPE '!'))");
            var termo = "%" + busca.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            args.add(termo); args.add(termo);
        }
        return new Filtro(sql.toString(), args);
    }

    public List<UUID> ids(Filtro f, int pagina, int tamanho) {
        var args = new ArrayList<>(f.args()); args.add(tamanho); args.add(pagina * tamanho);
        return jdbc.queryForList("SELECT c.id FROM contato c WHERE " + f.sql() + " ORDER BY LOWER(c.nome),c.id LIMIT ? OFFSET ?", UUID.class, args.toArray());
    }
    public long total(Filtro f) { return jdbc.queryForObject("SELECT COUNT(*) FROM contato c WHERE " + f.sql(), Long.class, f.args().toArray()); }
    public List<String> estados(UUID empresa) {
        return jdbc.queryForList("SELECT DISTINCT d.valor FROM contato_dado d JOIN contato c ON c.id=d.contato_id WHERE c.empresa_id=? AND c.tipo='Parceiro' AND d.campo='uf' AND d.valor<>'' ORDER BY d.valor", String.class, empresa);
    }
}
