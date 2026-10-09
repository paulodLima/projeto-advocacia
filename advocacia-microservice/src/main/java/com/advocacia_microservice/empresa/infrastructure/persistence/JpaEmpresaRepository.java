package com.advocacia_microservice.empresa.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEmpresaRepository extends JpaRepository<EmpresaEntity, UUID> {
    boolean existsByCnpjAndIdNot(String cnpj, UUID id);
}
