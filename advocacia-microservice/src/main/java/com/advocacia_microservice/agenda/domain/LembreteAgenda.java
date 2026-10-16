package com.advocacia_microservice.agenda.domain;
import java.time.*;
import java.util.UUID;
public record LembreteAgenda(UUID atividadeId,long versao,String titulo,TipoAtividade tipo,MarcoLembrete marco,LocalDate data,LocalTime hora) {}
