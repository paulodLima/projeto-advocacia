package com.advocacia_microservice.auth.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "codigo_acesso")
public class CodigoAcessoEntity {
    @Id
    public UUID usuarioId;
    @Column(nullable = false, unique = true)
    public UUID desafioId;
    @Column(nullable = false, length = 100)
    public String hash;
    @Column(nullable = false)
    public Instant expiraEm;
    @Column(nullable = false)
    public Instant enviadoEm;
    @Column(nullable = false)
    public int tentativas;
    @Column(nullable = false)
    public boolean utilizado;
    @Column(nullable = false)
    public Instant janelaInicio;
    @Column(nullable = false)
    public int enviosNaJanela;

    public CodigoAcessoEntity() {}
}
