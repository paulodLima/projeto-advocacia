package com.advocacia_microservice.plataforma.infrastructure.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.advocacia_microservice.plataforma.infrastructure.persistence.entity.*;

public interface AdministradorRepository extends JpaRepository<AdministradorEntity, UUID> {
    boolean existsByUsuarioIdAndAtivoTrue(UUID usuarioId);
}
