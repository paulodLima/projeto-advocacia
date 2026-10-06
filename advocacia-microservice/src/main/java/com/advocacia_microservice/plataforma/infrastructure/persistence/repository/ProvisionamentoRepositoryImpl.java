package com.advocacia_microservice.plataforma.infrastructure.persistence.repository;
import com.advocacia_microservice.plataforma.domain.repository.ProvisionamentoRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class ProvisionamentoRepositoryImpl implements ProvisionamentoRepository {
    private final JdbcTemplate jdbc;
    public ProvisionamentoRepositoryImpl(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public boolean bloquearEConsultarConclusao() {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT concluido FROM plataforma_provisionamento WHERE id = 1 FOR UPDATE", Boolean.class));
    }
    public void concluir() { jdbc.update("UPDATE plataforma_provisionamento SET concluido = TRUE WHERE id = 1"); }
}
