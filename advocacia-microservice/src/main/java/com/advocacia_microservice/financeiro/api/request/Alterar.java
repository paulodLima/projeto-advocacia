package com.advocacia_microservice.financeiro.api.request;

import com.advocacia_microservice.financeiro.application.FinanceiroConfigService;
import com.advocacia_microservice.financeiro.application.FinanceiroConfigService.Dados;
import com.advocacia_microservice.financeiro.domain.FinanceiroConfig.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record Alterar<T>(@NotNull @Min(0) Long versao, @NotNull T dados) {}
