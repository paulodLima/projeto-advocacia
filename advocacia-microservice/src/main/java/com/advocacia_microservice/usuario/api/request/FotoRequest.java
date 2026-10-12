package com.advocacia_microservice.usuario.api.request;

import com.advocacia_microservice.usuario.application.MeuPerfilService;
import com.advocacia_microservice.usuario.domain.UsuarioPerfil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record FotoRequest(@NotNull @Size(max = 700_000) String foto) {}
