package com.advocacia_microservice.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleIdentityRepository extends JpaRepository<GoogleIdentityEntity, UUID> {
    Optional<GoogleIdentityEntity> findBySubject(String subject);
}
