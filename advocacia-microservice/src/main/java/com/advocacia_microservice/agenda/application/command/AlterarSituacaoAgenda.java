package com.advocacia_microservice.agenda.application.command;
import com.advocacia_microservice.agenda.domain.SituacaoAgenda;
public record AlterarSituacaoAgenda(long versao,SituacaoAgenda situacao,String observacao) {}
