package com.advocacia_microservice.agenda.application.command;
import java.time.LocalDate;
import java.util.*;
public record PlanejarWorkflow(UUID workflowId,int versao,UUID tipoTarefaId,LocalDate prazoFatal,List<LocalDate> diasNaoUteis) {
    public PlanejarWorkflow {
        if(workflowId==null || tipoTarefaId==null || prazoFatal==null || versao<0 || prazoFatal.getYear()<1 || prazoFatal.getYear()>9999)
            throw new IllegalArgumentException("Escolha o workflow, sua versão, o tipo de tarefa e o prazo fatal manual.");
        diasNaoUteis=diasNaoUteis==null?List.of():diasNaoUteis;
        if(diasNaoUteis.size()>366 || diasNaoUteis.stream().anyMatch(Objects::isNull) || new HashSet<>(diasNaoUteis).size()!=diasNaoUteis.size())
            throw new IllegalArgumentException("Informe até 366 datas sem expediente, sem duplicidade.");
        diasNaoUteis=diasNaoUteis.stream().sorted().toList();
    }
}
