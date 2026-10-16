package com.advocacia_microservice.agenda.api.request;
import com.advocacia_microservice.agenda.domain.PreferenciaLembrete;
import jakarta.validation.constraints.NotNull;
public record PreferenciasLembreteRequest(@NotNull Boolean ativo,@NotNull Integer antecedenciaMinutos) {
    public PreferenciaLembrete dominio() {return new PreferenciaLembrete(ativo,antecedenciaMinutos);}
}
