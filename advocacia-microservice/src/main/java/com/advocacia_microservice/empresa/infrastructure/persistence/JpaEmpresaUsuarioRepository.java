package com.advocacia_microservice.empresa.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEmpresaUsuarioRepository extends JpaRepository<EmpresaUsuarioEntity, UUID> {
    java.util.List<EmpresaUsuarioEntity> findByEmpresaId(UUID empresaId);
}
