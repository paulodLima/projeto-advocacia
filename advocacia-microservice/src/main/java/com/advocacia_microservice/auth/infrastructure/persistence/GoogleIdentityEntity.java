package com.advocacia_microservice.auth.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "google_identity")
public class GoogleIdentityEntity {
    @Id
    public UUID usuarioId;
    @Column(nullable = false, unique = true, length = 255)
    public String subject;

    public GoogleIdentityEntity() {}
}
