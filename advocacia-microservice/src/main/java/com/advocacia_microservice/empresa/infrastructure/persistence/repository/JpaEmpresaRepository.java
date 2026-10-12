package com.advocacia_microservice.empresa.infrastructure.persistence.repository;
import com.advocacia_microservice.empresa.infrastructure.persistence.entity.EmpresaEntity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEmpresaRepository extends JpaRepository<EmpresaEntity, UUID> {
    boolean existsByCnpjAndIdNot(String cnpj, UUID id);
}
