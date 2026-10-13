package com.advocacia_microservice.agenda.api.response;
import com.advocacia_microservice.agenda.domain.DiaAgenda;
import java.time.LocalDate;
public record DiaAgendaResponse(LocalDate data,long total,long pendentes,long prazosFatais) {
    public static DiaAgendaResponse de(DiaAgenda d) {return new DiaAgendaResponse(d.data(),d.total(),d.pendentes(),d.prazosFatais());}
}
