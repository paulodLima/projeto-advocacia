package com.advocacia_microservice.empresa.infrastructure.persistence.repository;
import com.advocacia_microservice.empresa.infrastructure.persistence.entity.EmpresaUsuarioEntity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEmpresaUsuarioRepository extends JpaRepository<EmpresaUsuarioEntity, UUID> {
    java.util.List<EmpresaUsuarioEntity> findByEmpresaId(UUID empresaId);
}
