package com.advocacia_microservice.escritorio.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "escritorio_membro", uniqueConstraints = @UniqueConstraint(
        name = "uk_escritorio_usuario", columnNames = {"escritorio_id", "usuario_id"}))
public class MembroEntity {
    @Id public UUID id;
    @Column(name = "escritorio_id", nullable = false) public UUID escritorioId;
    @Column(name = "usuario_id", nullable = false) public UUID usuarioId;
    @Column(nullable = false, length = 30) public String perfil;
    @Column(nullable = false) public boolean ativo;
}
