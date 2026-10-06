package com.advocacia_microservice.auth.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.advocacia_microservice.auth.infrastructure.persistence.entity.*;

public interface GoogleIdentityRepository extends JpaRepository<GoogleIdentityEntity, UUID> {
    Optional<GoogleIdentityEntity> findBySubject(String subject);
}
