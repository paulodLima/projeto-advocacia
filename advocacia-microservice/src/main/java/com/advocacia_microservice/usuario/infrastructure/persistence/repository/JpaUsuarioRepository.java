package com.advocacia_microservice.usuario.infrastructure.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.advocacia_microservice.usuario.infrastructure.persistence.entity.*;

public interface JpaUsuarioRepository extends JpaRepository<UsuarioEntity, UUID> {
    java.util.Optional<UsuarioEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
}
