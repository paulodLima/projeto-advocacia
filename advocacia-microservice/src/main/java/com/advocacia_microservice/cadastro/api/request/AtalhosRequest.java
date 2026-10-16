package com.advocacia_microservice.cadastro.api.request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
public record AtalhosRequest(@NotNull @Size(max = 14) List<@NotNull String> atalhos) {}
