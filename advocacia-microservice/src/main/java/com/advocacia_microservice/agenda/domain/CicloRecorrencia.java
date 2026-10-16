package com.advocacia_microservice.agenda.domain;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
public record CicloRecorrencia(LocalDate base,int indice) {
    public DadosAtividade proxima(DadosAtividade d) {
        var data=d.repeticao().data(base,Math.addExact(indice,1));
        long dias=ChronoUnit.DAYS.between(d.data(),data);
        if(dias<=0) throw new IllegalArgumentException("A próxima ocorrência deve ser posterior à atual.");
        return new DadosAtividade(d.tipo(),d.titulo(),d.tipoTarefaId(),data,d.dataFim()==null?null:d.dataFim().plusDays(dias),
            d.hora(),d.horaFim(),d.prazoFatal()==null?null:d.prazoFatal().plusDays(dias),d.diaInteiro(),d.modalidade(),d.local(),d.descricao(),d.responsaveis(),d.participantes(),d.repeticao());
    }
}
