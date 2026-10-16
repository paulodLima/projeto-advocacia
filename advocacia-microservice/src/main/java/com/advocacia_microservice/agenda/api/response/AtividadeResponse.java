package com.advocacia_microservice.agenda.api.response;
import com.advocacia_microservice.agenda.domain.*;
import java.time.Instant;
import java.util.*;
public record AtividadeResponse(UUID id,long versao,DadosAtividade dados,SituacaoAgenda situacao,Instant concluidaEm,
        Instant criadoEm,Instant atualizadoEm,List<EventoAgenda> historico,OrigemWorkflow workflow) {
    public static AtividadeResponse de(AtividadeAgenda a) {return new AtividadeResponse(a.id(),a.versao(),a.dados(),a.situacao(),a.concluidaEm(),a.criadoEm(),a.atualizadoEm(),a.historico(),a.workflow());}
}
