package com.advocacia_microservice.plataforma.api.request;
import jakarta.validation.constraints.*;

public record ConvidarResponsavelRequest(@NotBlank @Size(max=150) String nome, @NotBlank @Email @Size(max=254) String email) {}
