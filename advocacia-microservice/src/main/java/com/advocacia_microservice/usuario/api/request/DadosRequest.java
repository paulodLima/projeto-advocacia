package com.advocacia_microservice.usuario.api.request;

import com.advocacia_microservice.usuario.application.MeuPerfilService;
import com.advocacia_microservice.usuario.domain.UsuarioPerfil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record DadosRequest(
            @NotNull @Size(max = 30) String telefone,
            @NotNull @Email @Size(max = 254) String emailPessoal,
            @NotNull @Size(max = 500) String endereco) {}
