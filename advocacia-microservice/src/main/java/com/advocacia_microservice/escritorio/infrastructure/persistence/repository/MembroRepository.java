package com.advocacia_microservice.escritorio.infrastructure.persistence.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.advocacia_microservice.escritorio.infrastructure.persistence.entity.*;

public interface MembroRepository extends JpaRepository<MembroEntity, UUID> {
    List<MembroEntity> findByUsuarioIdAndAtivoTrue(UUID usuarioId);
    Optional<MembroEntity> findByEscritorioIdAndUsuarioIdAndAtivoTrue(UUID escritorioId, UUID usuarioId);
}
