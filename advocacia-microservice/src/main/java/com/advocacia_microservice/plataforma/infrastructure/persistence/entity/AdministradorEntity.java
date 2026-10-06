package com.advocacia_microservice.plataforma.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "administrador_plataforma")
public class AdministradorEntity {
    @Id @Column(name = "usuario_id") public UUID usuarioId;
    @Column(nullable = false) public boolean ativo;
}
