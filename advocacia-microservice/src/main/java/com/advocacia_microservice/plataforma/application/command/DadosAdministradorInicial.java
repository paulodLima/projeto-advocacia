package com.advocacia_microservice.plataforma.application.command;
import jakarta.validation.constraints.*;
public record DadosAdministradorInicial(@NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 150) String nome) {}
