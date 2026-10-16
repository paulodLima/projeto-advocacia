package com.advocacia_microservice.agenda.api.response;
import com.advocacia_microservice.agenda.domain.PlanoWorkflow;
import java.time.LocalDate;
import java.util.*;
public record PlanoWorkflowResponse(String assinatura,UUID workflowId,String nome,int versao,UUID tipoTarefaId,LocalDate prazoFatal,int buffer,List<LocalDate> diasNaoUteis,List<EtapaPlanejadaResponse> etapas) {
    public static PlanoWorkflowResponse de(PlanoWorkflow p){return new PlanoWorkflowResponse(p.assinatura(),p.workflowId(),p.nome(),p.versao(),p.tipoTarefaId(),p.prazoFatal(),p.buffer(),p.diasNaoUteis(),p.etapas().stream().map(EtapaPlanejadaResponse::de).toList());}
}
