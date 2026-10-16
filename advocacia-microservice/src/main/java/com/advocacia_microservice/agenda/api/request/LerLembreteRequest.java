package com.advocacia_microservice.agenda.api.request;
import com.advocacia_microservice.agenda.domain.MarcoLembrete;
import com.advocacia_microservice.agenda.application.command.LerLembrete;
import jakarta.validation.constraints.*;
public record LerLembreteRequest(@NotNull @Min(0) Long versao,@NotNull MarcoLembrete marco) {
    public LerLembrete comando() {return new LerLembrete(versao,marco);}
}
