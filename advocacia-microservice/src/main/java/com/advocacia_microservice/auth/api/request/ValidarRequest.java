package com.advocacia_microservice.auth.api.request;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record ValidarRequest(@NotNull UUID desafioId, @NotBlank @Pattern(regexp = "[0-9]{6}") String codigo) {}
