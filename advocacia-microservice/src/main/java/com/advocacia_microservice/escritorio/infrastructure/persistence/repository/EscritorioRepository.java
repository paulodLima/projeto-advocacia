package com.advocacia_microservice.escritorio.infrastructure.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.advocacia_microservice.escritorio.infrastructure.persistence.entity.*;

public interface EscritorioRepository extends JpaRepository<EscritorioEntity, UUID> {}
