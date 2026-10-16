package com.advocacia_microservice.agenda.api.request;
import com.advocacia_microservice.agenda.application.command.SalvarAtividade;
import com.advocacia_microservice.agenda.domain.TipoAtividade;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
public record SalvarAtividadeRequest(@Min(0) long versao,@NotNull TipoAtividade tipo,@Size(max=150) String titulo,
        UUID tipoTarefaId,LocalDate data,LocalDate dataFim,LocalTime hora,LocalTime horaFim,LocalDate prazoFatal,
        boolean diaInteiro,String modalidade,@Size(max=1000) String local,@Size(max=4000) String descricao,
        @NotNull @Size(max=20) List<@NotNull UUID> responsaveis, @Size(max=50) List<@NotNull UUID> participantes) {
    public SalvarAtividade comando() {return new SalvarAtividade(versao,tipo,titulo,tipoTarefaId,data,dataFim,hora,horaFim,prazoFatal,diaInteiro,modalidade,local,descricao,responsaveis,participantes);}
}
