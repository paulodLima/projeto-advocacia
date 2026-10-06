package com.advocacia_microservice.plataforma.infrastructure.persistence.repository;
import com.advocacia_microservice.plataforma.domain.repository.AuditoriaRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class AuditoriaRepositoryImpl implements AuditoriaRepository {
    private final JdbcTemplate jdbc;
    public AuditoriaRepositoryImpl(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void registrar(UUID autorId, String acao, UUID alvoId) {
        jdbc.update("INSERT INTO plataforma_auditoria (id, autor_id, acao, alvo_id, registrado_em) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), autorId, acao, alvoId, java.sql.Timestamp.from(Instant.now()));
    }
}
