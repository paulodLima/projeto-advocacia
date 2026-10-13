package com.advocacia_microservice.agenda.application.command;
import com.advocacia_microservice.agenda.domain.TipoAtividade;
import java.time.*;
import java.util.*;
public record SalvarAtividade(long versao,TipoAtividade tipo,String titulo,UUID tipoTarefaId,LocalDate data,
        LocalDate dataFim,LocalTime hora,LocalTime horaFim,LocalDate prazoFatal,boolean diaInteiro,
        String modalidade,String local,String descricao,List<UUID> responsaveis) {}
