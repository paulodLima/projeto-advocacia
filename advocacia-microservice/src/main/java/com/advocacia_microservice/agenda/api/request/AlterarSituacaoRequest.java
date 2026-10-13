package com.advocacia_microservice.agenda.api.request;
import com.advocacia_microservice.agenda.application.command.AlterarSituacaoAgenda;
import com.advocacia_microservice.agenda.domain.SituacaoAgenda;
import jakarta.validation.constraints.*;
public record AlterarSituacaoRequest(@Min(0) long versao,@NotNull SituacaoAgenda situacao,@Size(max=2000) String observacao) {
    public AlterarSituacaoAgenda comando() {return new AlterarSituacaoAgenda(versao,situacao,observacao);}
}
