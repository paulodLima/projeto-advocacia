package com.advocacia_microservice.agenda.domain;
import java.time.*;
import java.util.UUID;
public record ProjecaoAgenda(UUID empresa,UUID id,UUID lead,UUID tarefa,UUID caso,String titulo,
 LocalDate data,LocalDate dataFim,LocalTime hora,LocalTime horaFim,LocalDate prazoFatal,boolean diaInteiro,
 String local,String situacao,boolean mostrar,boolean privada,UUID autor) {}
