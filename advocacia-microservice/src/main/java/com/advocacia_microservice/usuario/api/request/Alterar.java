package com.advocacia_microservice.usuario.api.request;

import com.advocacia_microservice.usuario.application.TemaUsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record Alterar(@NotBlank @Size(max=10) String tema) {}
