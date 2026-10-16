package com.advocacia_microservice.agenda.domain;
import java.util.*;
public record ExecucaoWorkflow(UUID id,String assinatura,PlanoWorkflow plano,List<UUID> atividades) {
    public ExecucaoWorkflow {atividades=List.copyOf(atividades);}
}
