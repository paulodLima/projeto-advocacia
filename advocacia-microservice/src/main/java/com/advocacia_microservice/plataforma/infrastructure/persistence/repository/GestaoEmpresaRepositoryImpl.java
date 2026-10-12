package com.advocacia_microservice.plataforma.infrastructure.persistence.repository;

import com.advocacia_microservice.plataforma.domain.GestaoEmpresa;
import com.advocacia_microservice.plataforma.domain.repository.GestaoEmpresaRepository;
import com.advocacia_microservice.shared.exception.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GestaoEmpresaRepositoryImpl implements GestaoEmpresaRepository {
    private final JdbcTemplate jdbc;
    public GestaoEmpresaRepositoryImpl(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private static final String BASE = """
        SELECT e.id,e.nome,e.cnpj, COALESCE(c.situacao,CASE WHEN e.ativo THEN 'ATIVA' ELSE 'SUSPENSA' END) situacao,
            COALESCE(c.financeiro,'NAO_INFORMADO') financeiro,c.inicio,COALESCE(c.versao,0) versao,
            (SELECT u.nome FROM empresa_usuario v JOIN usuario u ON u.id=v.usuario_id WHERE v.empresa_id=e.id AND v.papel='MASTER' ORDER BY u.id LIMIT 1) responsavel,
            (SELECT u.email FROM empresa_usuario v JOIN usuario u ON u.id=v.usuario_id WHERE v.empresa_id=e.id AND v.papel='MASTER' ORDER BY u.id LIMIT 1) email_responsavel,
            (SELECT COUNT(*) FROM empresa_usuario v JOIN usuario u ON u.id=v.usuario_id LEFT JOIN equipe_membro m ON m.usuario_id=u.id WHERE v.empresa_id=e.id AND u.status='ATIVO' AND COALESCE(m.situacao,'ATIVO')='ATIVO') usuarios
        FROM escritorio e LEFT JOIN empresa_contrato c ON c.empresa_id=e.id
        """;
    public List<GestaoEmpresa> listar(String busca, String situacao, int pagina) {
        return jdbc.query(BASE + " WHERE (LOWER(e.nome) LIKE ? ESCAPE '!' OR e.cnpj LIKE ? ESCAPE '!') AND (?='' OR COALESCE(c.situacao,CASE WHEN e.ativo THEN 'ATIVA' ELSE 'SUSPENSA' END)=?) ORDER BY e.nome,e.id LIMIT 20 OFFSET ?",
            (r,n) -> new GestaoEmpresa(r.getObject("id",UUID.class),r.getString("nome"),r.getString("cnpj"),r.getString("responsavel"),r.getString("email_responsavel"),r.getLong("usuarios"),r.getString("situacao"),r.getString("financeiro"),r.getObject("inicio",LocalDate.class),r.getLong("versao"),List.of(),List.of()),
            "%"+busca.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%",
            "%"+busca.replace("!","!!").replace("%","!%").replace("_","!_")+"%",situacao,situacao,(long)pagina*20);
    }
    public GestaoEmpresa buscar(UUID id) {
        var lista = jdbc.query(BASE + " WHERE e.id=?", (r,n) -> new GestaoEmpresa(id,r.getString("nome"),r.getString("cnpj"),r.getString("responsavel"),r.getString("email_responsavel"),r.getLong("usuarios"),r.getString("situacao"),r.getString("financeiro"),r.getObject("inicio",LocalDate.class),r.getLong("versao"),modulos(id),historico(id)),id);
        if (lista.isEmpty()) throw new RecursoNaoEncontradoException("Empresa não encontrada.");
        return lista.getFirst();
    }
    private List<GestaoEmpresa.Modulo> modulos(UUID id) {
        return jdbc.query("SELECT m.codigo,m.nome,m.situacao,COALESCE(em.liberado,FALSE) liberado FROM modulo_plataforma m LEFT JOIN empresa_modulo em ON em.modulo_codigo=m.codigo AND em.empresa_id=? ORDER BY m.ordem",
            (r,n) -> new GestaoEmpresa.Modulo(r.getString(1),r.getString(2),r.getString(3),r.getBoolean(4)),id);
    }
    private List<GestaoEmpresa.Evento> historico(UUID id) {
        return jdbc.query("SELECT h.*,u.nome autor FROM empresa_contrato_evento h JOIN usuario u ON u.id=h.autor_id WHERE h.empresa_id=? ORDER BY h.criado_em DESC,h.id DESC",
            (r,n) -> new GestaoEmpresa.Evento(r.getObject("id",UUID.class),r.getString("autor"),r.getString("acao"),r.getString("anterior"),r.getString("atual"),r.getString("motivo"),r.getTimestamp("criado_em").toInstant()),id);
    }
    public void alterar(UUID id, long versao, String situacao, String financeiro, LocalDate inicio, Set<String> modulos, UUID autor, String motivo) {
        // Serializa também o primeiro contrato de empresas cadastradas após a migration.
        jdbc.queryForList("SELECT id FROM escritorio WHERE id=? FOR UPDATE",id);
        var anterior = buscar(id);
        if (anterior.versao()!=versao) throw new ConflitoException("A empresa foi alterada. Recarregue a ficha antes de salvar.");
        jdbc.update("INSERT INTO empresa_contrato(empresa_id,situacao) SELECT id,CASE WHEN ativo THEN 'ATIVA' ELSE 'SUSPENSA' END FROM escritorio WHERE id=? AND NOT EXISTS(SELECT 1 FROM empresa_contrato WHERE empresa_id=?)",id,id);
        jdbc.update("UPDATE empresa_contrato SET situacao=?,financeiro=?,inicio=?,versao=versao+1,atualizado_em=CURRENT_TIMESTAMP WHERE empresa_id=?",situacao,financeiro,inicio,id);
        jdbc.update("UPDATE escritorio SET ativo=? WHERE id=?","ATIVA".equals(situacao),id);
        String acao = anterior.situacao().equals(situacao) ? "CONTRATO_ATUALIZADO" : switch(situacao) { case "ATIVA" -> "EMPRESA_REATIVADA"; case "SUSPENSA" -> "EMPRESA_SUSPENSA"; default -> "CONTRATO_ENCERRADO"; };
        registrar(id,autor,acao,anterior.situacao(),situacao,motivo);
        if (!anterior.financeiro().equals(financeiro)) registrar(id,autor,"FINANCEIRO_ATUALIZADO",anterior.financeiro(),financeiro,motivo);
        if (!Objects.equals(anterior.inicio(),inicio)) registrar(id,autor,"INICIO_ATUALIZADO",Objects.toString(anterior.inicio(),""),Objects.toString(inicio,""),motivo);
        for (var m : anterior.modulos()) {
            boolean liberado = modulos.contains(m.codigo());
            if (m.liberado()!=liberado) registrar(id,autor,"MODULO_ALTERADO",m.liberado()?"LIBERADO":"BLOQUEADO",liberado?"LIBERADO":"BLOQUEADO",m.codigo()+": "+motivo);
            jdbc.update("DELETE FROM empresa_modulo WHERE empresa_id=? AND modulo_codigo=?",id,m.codigo());
            jdbc.update("INSERT INTO empresa_modulo(empresa_id,modulo_codigo,liberado) VALUES(?,?,?)",id,m.codigo(),liberado);
        }
    }
    private void registrar(UUID empresa,UUID autor,String acao,String anterior,String atual,String motivo) {
        jdbc.update("INSERT INTO empresa_contrato_evento(id,empresa_id,autor_id,acao,anterior,atual,motivo) VALUES(?,?,?,?,?,?,?)",UUID.randomUUID(),empresa,autor,acao,anterior,atual,motivo);
    }
    public Set<String> modulosPermitidos(UUID id) { return modulos(id).stream().filter(GestaoEmpresa.Modulo::liberado).map(GestaoEmpresa.Modulo::codigo).collect(java.util.stream.Collectors.toSet()); }
}
