package com.advocacia_microservice.equipe.api.request;
import com.advocacia_microservice.equipe.api.ConfiguracaoMembroRequest;

import com.advocacia_microservice.equipe.application.EquipeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record ConviteRequest(@NotBlank @Size(max = 150) String nome,
            @NotBlank @Email @Size(max = 254) String email, @NotNull @Valid ConfiguracaoMembroRequest configuracao) {}
