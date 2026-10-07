package com.advocacia_microservice.financeiro.application;

import com.advocacia_microservice.financeiro.domain.FinanceiroConfig.*;
import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.PapelEmpresa;
import com.advocacia_microservice.shared.exception.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @Transactional(readOnly = true)
public class FinanceiroConfigService {
    public record Dados(UUID empresaId, PapelEmpresa papel, long versao, List<Conta> contas, List<Categoria> categorias, List<Centro> centros, List<Custo> custos, BigDecimal urh, String competencia, Capacidade capacidade) {}
    private final EmpresaService empresas;
    private final JdbcTemplate jdbc;
    public FinanceiroConfigService(EmpresaService empresas, JdbcTemplate jdbc) { this.empresas = empresas; this.jdbc = jdbc; }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Dados buscar(UUID usuario) {
        var empresa = empresas.buscar(usuario);
        if (empresa.id() == null) return new Dados(null, null, 0, List.of(), List.of(), List.of(), List.of(), BigDecimal.ZERO, "", Capacidade.padrao());
        UUID id = empresa.id();
        return jdbc.query("SELECT * FROM financeiro_config WHERE empresa_id=?", (rs, n) -> new Dados(id, empresa.papel(), rs.getLong("versao"), contas(id), categorias(id), centros(id), custos(id), rs.getBigDecimal("urh"), rs.getString("competencia"), new Capacidade(rs.getBigDecimal("horas"), rs.getString("modo_horas"), rs.getInt("advogados"), rs.getBigDecimal("horas_semanais"), rs.getBigDecimal("percentual_produtivo"), rs.getBigDecimal("semanas_por_mes"), rs.getBigDecimal("margem_lucro"), rs.getBigDecimal("fator_posicionamento"))), id).stream().findFirst().orElseGet(() -> new Dados(id, empresa.papel(), 0, List.of(), List.of(), List.of(), List.of(), BigDecimal.ZERO, "", Capacidade.padrao()));
    }
    private List<Conta> contas(UUID id) { return jdbc.query("SELECT * FROM financeiro_conta WHERE empresa_id=? ORDER BY nome_chave,id", (rs,n) -> new Conta(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getString("banco"),rs.getString("tipo"),rs.getBigDecimal("saldo"),rs.getBoolean("padrao"),rs.getBoolean("ativo")),id); }
    private List<Categoria> categorias(UUID id) { return jdbc.query("SELECT * FROM financeiro_categoria WHERE empresa_id=? ORDER BY nome_chave,id", (rs,n) -> new Categoria(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getString("grupo"),rs.getString("direcao"),rs.getBoolean("ativo")),id); }
    private List<Centro> centros(UUID id) { return jdbc.query("SELECT * FROM financeiro_centro WHERE empresa_id=? ORDER BY nome_chave,id", (rs,n) -> new Centro(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getBoolean("ativo")),id); }
    private List<Custo> custos(UUID id) { return jdbc.query("SELECT * FROM financeiro_custo WHERE empresa_id=? ORDER BY nome_chave,id", (rs,n) -> new Custo(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getBigDecimal("valor")),id); }
    private UUID editar(UUID usuario, long versao) {
        UUID id = empresas.exigirMaster(usuario).empresaId();
        jdbc.queryForObject("SELECT id FROM empresa WHERE id=? FOR UPDATE", UUID.class, id);
        long atual = jdbc.query("SELECT versao FROM financeiro_config WHERE empresa_id=?", (rs,n) -> rs.getLong(1),id).stream().findFirst().orElse(0L);
        if (versao < 0 || atual != versao) throw new ConflitoException("O financeiro foi alterado em outra sessão. Recarregue os dados antes de salvar.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM financeiro_config WHERE empresa_id=?",Integer.class,id) == 0) jdbc.update("INSERT INTO financeiro_config(empresa_id) VALUES (?)",id);
        jdbc.update("UPDATE financeiro_config SET versao=versao+1 WHERE empresa_id=?",id);
        return id;
    }
    private void exigir(UUID empresa, UUID id, String tabela) {
        // Tabela vem exclusivamente das constantes dos métodos abaixo.
        if (jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela + " WHERE empresa_id=? AND id=?",Integer.class,empresa,id) == 0) throw new RecursoNaoEncontradoException("Cadastro financeiro não encontrado na sua empresa.");
    }
    private String chave(String nome) { return nome.toLowerCase(Locale.ROOT); }
    @Transactional public Dados conta(UUID usuario, long versao, UUID id, Conta c) {
        UUID empresa = editar(usuario,versao); boolean nova = id == null;
        if (nova) id = UUID.randomUUID(); else exigir(empresa,id,"financeiro_conta");
        if (c.padrao()) jdbc.update("UPDATE financeiro_conta SET padrao=false WHERE empresa_id=?",empresa);
        if (nova) jdbc.update("INSERT INTO financeiro_conta(id,empresa_id,nome,nome_chave,banco,tipo,saldo,padrao,ativo) VALUES (?,?,?,?,?,?,?,?,?)",id,empresa,c.nome(),chave(c.nome()),c.banco(),c.tipo(),c.saldo(),c.padrao(),c.ativo());
        else jdbc.update("UPDATE financeiro_conta SET nome=?,nome_chave=?,banco=?,tipo=?,saldo=?,padrao=?,ativo=? WHERE empresa_id=? AND id=?",c.nome(),chave(c.nome()),c.banco(),c.tipo(),c.saldo(),c.padrao(),c.ativo(),empresa,id);
        return buscar(usuario);
    }
    @Transactional public Dados categoria(UUID usuario, long versao, UUID id, Categoria c) {
        UUID empresa = editar(usuario,versao); boolean nova = id == null;
        if (nova) id = UUID.randomUUID(); else exigir(empresa,id,"financeiro_categoria");
        if (nova) jdbc.update("INSERT INTO financeiro_categoria(id,empresa_id,nome,nome_chave,grupo,direcao,ativo) VALUES (?,?,?,?,?,?,?)",id,empresa,c.nome(),chave(c.nome()),c.grupo(),c.direcao(),c.ativo());
        else jdbc.update("UPDATE financeiro_categoria SET nome=?,nome_chave=?,grupo=?,direcao=?,ativo=? WHERE empresa_id=? AND id=?",c.nome(),chave(c.nome()),c.grupo(),c.direcao(),c.ativo(),empresa,id);
        return buscar(usuario);
    }
    @Transactional public Dados centro(UUID usuario, long versao, UUID id, Centro c) {
        UUID empresa = editar(usuario,versao); boolean novo = id == null;
        if (novo) id = UUID.randomUUID(); else exigir(empresa,id,"financeiro_centro");
        if (novo) jdbc.update("INSERT INTO financeiro_centro(id,empresa_id,nome,nome_chave,ativo) VALUES (?,?,?,?,?)",id,empresa,c.nome(),chave(c.nome()),c.ativo());
        else jdbc.update("UPDATE financeiro_centro SET nome=?,nome_chave=?,ativo=? WHERE empresa_id=? AND id=?",c.nome(),chave(c.nome()),c.ativo(),empresa,id);
        return buscar(usuario);
    }
    @Transactional public Dados custo(UUID usuario, long versao, Custo c) {
        UUID empresa = editar(usuario,versao);
        jdbc.update("INSERT INTO financeiro_custo(id,empresa_id,nome,nome_chave,valor) VALUES (?,?,?,?,?)",UUID.randomUUID(),empresa,c.nome(),chave(c.nome()),c.valor()); return buscar(usuario);
    }
    @Transactional public Dados excluirCusto(UUID usuario, long versao, UUID id) {
        UUID empresa = editar(usuario,versao); exigir(empresa,id,"financeiro_custo");
        jdbc.update("DELETE FROM financeiro_custo WHERE empresa_id=? AND id=?",empresa,id); return buscar(usuario);
    }
    @Transactional public Dados urh(UUID usuario, long versao, Urh u) {
        UUID empresa = editar(usuario,versao);
        jdbc.update("UPDATE financeiro_config SET urh=?,competencia=? WHERE empresa_id=?",u.urh(),u.competencia(),empresa); return buscar(usuario);
    }
    @Transactional public Dados capacidade(UUID usuario, long versao, Capacidade c) {
        UUID empresa = editar(usuario,versao);
        jdbc.update("UPDATE financeiro_config SET horas=?,modo_horas=?,advogados=?,horas_semanais=?,percentual_produtivo=?,semanas_por_mes=?,margem_lucro=?,fator_posicionamento=? WHERE empresa_id=?",c.horas(),c.modoHoras(),c.advogados(),c.horasSemanais(),c.percentualProdutivo(),c.semanasPorMes(),c.margemLucro(),c.fatorPosicionamento(),empresa); return buscar(usuario);
    }
}
