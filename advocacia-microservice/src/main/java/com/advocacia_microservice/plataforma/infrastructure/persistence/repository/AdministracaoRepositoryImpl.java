package com.advocacia_microservice.plataforma.infrastructure.persistence.repository;
import com.advocacia_microservice.plataforma.domain.repository.AdministracaoRepository;
import com.advocacia_microservice.plataforma.infrastructure.persistence.entity.AdministradorEntity;
import java.util.UUID;
import org.springframework.stereotype.Repository;
@Repository
public class AdministracaoRepositoryImpl implements AdministracaoRepository {
    private final AdministradorRepository jpa;
    public AdministracaoRepositoryImpl(AdministradorRepository jpa) { this.jpa = jpa; }
    public boolean estaAtivo(UUID id) { return jpa.existsByUsuarioIdAndAtivoTrue(id); }
    public long contar() { return jpa.count(); }
    public void conceder(UUID id) {
        var e = new AdministradorEntity(); e.usuarioId = id; e.ativo = true; jpa.saveAndFlush(e);
    }
}
