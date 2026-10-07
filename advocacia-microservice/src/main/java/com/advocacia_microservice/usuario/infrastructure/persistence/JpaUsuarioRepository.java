package com.advocacia_microservice.usuario.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUsuarioRepository extends JpaRepository<UsuarioEntity, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from UsuarioEntity u where u.id = :id")
    java.util.Optional<UsuarioEntity> buscarParaAtualizar(@org.springframework.data.repository.query.Param("id") UUID id);
    java.util.Optional<UsuarioEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
}
