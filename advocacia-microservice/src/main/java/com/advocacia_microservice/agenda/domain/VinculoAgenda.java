package com.advocacia_microservice.agenda.domain;
import java.time.*;
import java.util.*;
public record VinculoAgenda(UUID id,UUID leadId,UUID tarefaId,UUID casoId,String titulo,TipoAtividade tipo,
 LocalDate data,LocalDate dataFim,LocalTime hora,LocalTime horaFim,LocalDate prazoFatal,boolean diaInteiro,
 String local,SituacaoAgenda situacao,List<OpcaoAgenda> responsaveis) {}
