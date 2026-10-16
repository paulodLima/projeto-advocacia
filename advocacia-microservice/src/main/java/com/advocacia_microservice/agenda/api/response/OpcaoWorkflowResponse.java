package com.advocacia_microservice.agenda.api.response;
import com.advocacia_microservice.agenda.domain.ModeloWorkflow;
import java.util.UUID;
public record OpcaoWorkflowResponse(UUID id,String nome,int versao,int buffer,int quantidadeEtapas) {
    public static OpcaoWorkflowResponse de(ModeloWorkflow m){return new OpcaoWorkflowResponse(m.id(),m.nome(),m.versao(),m.buffer(),m.etapas().size());}
}
