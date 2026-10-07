package com.advocacia_microservice.equipe.infrastructure.persistence;

import com.advocacia_microservice.equipe.domain.PerfilEquipe;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity @Table(name = "equipe_convite")
public class ConviteEquipeEntity {
    @Id public UUID id;
    @Column(name = "empresa_id", nullable = false) public UUID empresaId;
    @Column(nullable = false, length = 254) public String email;
    @Column(nullable = false, length = 150) public String nome;
    @Column(name = "nome_exibicao", nullable = false, length = 150) public String nomeExibicao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public PerfilEquipe perfil;
    @Column(nullable = false) public boolean restrito;
    @Column(name = "envia_documento", nullable = false) public boolean enviaDocumento;
    @Column(nullable = false, length = 20) public String status;
    @Column(name = "expira_em", nullable = false) public Instant expiraEm;
    @ElementCollection @CollectionTable(name = "equipe_convite_aba", joinColumns = @JoinColumn(name = "convite_id"))
    @Column(name = "aba", nullable = false, length = 30) public Set<String> abas = new HashSet<>();
}
