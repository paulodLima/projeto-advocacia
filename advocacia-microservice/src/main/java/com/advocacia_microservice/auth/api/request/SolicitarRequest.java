package com.advocacia_microservice.auth.api.request;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record SolicitarRequest(@NotBlank @Email @Size(max = 254) String email) {}
