package com.advocacia_microservice.auth.api.request;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record CadastroRequest(@NotBlank @Size(max = 150) String nome) {}
