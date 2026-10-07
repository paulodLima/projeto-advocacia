package com.advocacia_microservice.equipe.infrastructure.persistence;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MembroEquipeRepository extends JpaRepository<MembroEquipeEntity, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "abas")
    java.util.List<MembroEquipeEntity> findByUsuarioIdIn(java.util.Set<UUID> ids);
}
