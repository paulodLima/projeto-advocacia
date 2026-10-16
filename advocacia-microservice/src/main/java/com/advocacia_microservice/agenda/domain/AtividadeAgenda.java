package com.advocacia_microservice.agenda.domain;
import java.time.Instant;
import java.util.*;
public record AtividadeAgenda(UUID id, UUID empresaId, long versao, DadosAtividade dados, SituacaoAgenda situacao,
        Instant concluidaEm, Instant criadoEm, Instant atualizadoEm, List<EventoAgenda> historico,OrigemWorkflow workflow) {
    public AtividadeAgenda(UUID id,UUID empresaId,long versao,DadosAtividade dados,SituacaoAgenda situacao,Instant concluidaEm,Instant criadoEm,Instant atualizadoEm,List<EventoAgenda> historico) {
        this(id,empresaId,versao,dados,situacao,concluidaEm,criadoEm,atualizadoEm,historico,null);
    }
}
