package com.advocacia_microservice.auth.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CodigoAcessoRepository extends JpaRepository<CodigoAcessoEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CodigoAcessoEntity c where c.email = :email")
    Optional<CodigoAcessoEntity> bloquearPorEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CodigoAcessoEntity c where c.desafioId = :id")
    Optional<CodigoAcessoEntity> bloquearPorDesafio(UUID id);
}
