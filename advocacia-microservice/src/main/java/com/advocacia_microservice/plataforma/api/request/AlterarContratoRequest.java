package com.advocacia_microservice.plataforma.api.request;
import com.advocacia_microservice.plataforma.application.command.AlterarContrato;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.Set;
public record AlterarContratoRequest(@Min(0) long versao, @NotBlank String situacao, @NotBlank String financeiro,
        LocalDate inicio, @NotNull Set<String> modulos, @NotBlank @Size(max=1000) String motivo) {
    public AlterarContrato comando() { return new AlterarContrato(versao,situacao,financeiro,inicio,modulos,motivo); }
}
