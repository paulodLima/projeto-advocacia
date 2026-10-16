package com.advocacia_microservice.agenda.api.response;
import com.advocacia_microservice.agenda.domain.*;
import java.time.*;
import java.util.UUID;
public record LembreteAgendaResponse(UUID atividadeId,long versao,String titulo,TipoAtividade tipo,MarcoLembrete marco,LocalDate data,LocalTime hora) {
    public static LembreteAgendaResponse de(LembreteAgenda l) {return new LembreteAgendaResponse(l.atividadeId(),l.versao(),l.titulo(),l.tipo(),l.marco(),l.data(),l.hora());}
}
