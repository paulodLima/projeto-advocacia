package com.advocacia_microservice.agenda.api.response;
import com.advocacia_microservice.agenda.domain.ExecucaoWorkflow;
import java.util.*;
public record ExecucaoWorkflowResponse(UUID id,PlanoWorkflowResponse plano,List<UUID> atividades) {
    public static ExecucaoWorkflowResponse de(ExecucaoWorkflow e){return new ExecucaoWorkflowResponse(e.id(),PlanoWorkflowResponse.de(e.plano()),e.atividades());}
}
