package com.advocacia_microservice.empresa.infrastructure.persistence.repository;
import com.advocacia_microservice.empresa.infrastructure.persistence.entity.IdentidadeVisualEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface JpaIdentidadeVisualRepository extends JpaRepository<IdentidadeVisualEntity, UUID> {}
