package com.advocacia_microservice.equipe.infrastructure.persistence;

import com.advocacia_microservice.equipe.domain.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;

@Entity @Table(name = "equipe_membro")
public class MembroEquipeEntity {
    @Id @Column(name = "usuario_id") public UUID usuarioId;
    @Column(name = "nome_exibicao", nullable = false, length = 150) public String nomeExibicao = "";
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public PerfilEquipe perfil = PerfilEquipe.ASSISTENTE;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public SituacaoEquipe situacao = SituacaoEquipe.ATIVO;
    public LocalDate admissao;
    public LocalDate nascimento;
    @Column(nullable = false) public boolean restrito;
    @Column(name = "envia_documento", nullable = false) public boolean enviaDocumento;
    @ElementCollection @CollectionTable(name = "equipe_membro_aba", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "aba", nullable = false, length = 30) public Set<String> abas = new HashSet<>();
}
