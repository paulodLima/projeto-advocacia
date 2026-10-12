package com.advocacia_microservice.plataforma.infrastructure.persistence.repository;

import com.advocacia_microservice.plataforma.domain.ConviteInicial;
import com.advocacia_microservice.plataforma.domain.repository.ConviteInicialRepository;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ConviteInicialRepositoryImpl implements ConviteInicialRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<ConviteInicial> mapper = (rs, n) -> new ConviteInicial(
            rs.getObject("id", UUID.class), rs.getString("nome"), rs.getString("email"), rs.getString("status"),
            rs.getTimestamp("expira_em").toInstant(), rs.getObject("usuario_id", UUID.class), rs.getObject("empresa_id", UUID.class));
    public ConviteInicialRepositoryImpl(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<ConviteInicial> listar() { return jdbc.query("SELECT * FROM plataforma_convite_inicial ORDER BY expira_em DESC, id", mapper); }
    public Optional<ConviteInicial> buscar(UUID id) { return jdbc.query("SELECT * FROM plataforma_convite_inicial WHERE id=?", mapper, id).stream().findFirst(); }
    public Optional<ConviteInicial> buscarPorEmail(String email) { return buscarEmail(email, false); }
    public Optional<ConviteInicial> bloquearPorEmail(String email) { return buscarEmail(email, true); }
    private Optional<ConviteInicial> buscarEmail(String email, boolean bloquear) {
        return jdbc.query("SELECT * FROM plataforma_convite_inicial WHERE email=?" + (bloquear ? " FOR UPDATE" : ""), mapper, email).stream().findFirst();
    }
    public ConviteInicial salvar(ConviteInicial c) {
        int atualizados = jdbc.update("UPDATE plataforma_convite_inicial SET nome=?, email=?, status=?, expira_em=?, usuario_id=?, empresa_id=? WHERE id=?",
                c.nome(), c.email(), c.status(), java.sql.Timestamp.from(c.expiraEm()), c.usuarioId(), c.empresaId(), c.id());
        if (atualizados == 0) jdbc.update("INSERT INTO plataforma_convite_inicial(id,nome,email,status,expira_em,usuario_id,empresa_id) VALUES (?,?,?,?,?,?,?)",
                c.id(), c.nome(), c.email(), c.status(), java.sql.Timestamp.from(c.expiraEm()), c.usuarioId(), c.empresaId());
        return c;
    }
}
