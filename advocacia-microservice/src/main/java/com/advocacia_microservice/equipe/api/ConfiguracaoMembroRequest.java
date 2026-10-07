package com.advocacia_microservice.equipe.api;

import com.advocacia_microservice.equipe.domain.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.Set;

public record ConfiguracaoMembroRequest(
        @NotNull @Size(max = 150) String nomeExibicao,
        @NotNull @Size(max = 30) String telefone,
        LocalDate admissao, @PastOrPresent LocalDate nascimento,
        @NotNull PerfilEquipe perfil, @NotNull SituacaoEquipe situacao,
        boolean restrito, @NotNull @Size(max = 16) Set<@NotNull String> abas, boolean enviaDocumento) {}
