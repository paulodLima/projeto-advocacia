package com.advocacia_microservice.agenda.domain.repository;
import com.advocacia_microservice.agenda.domain.*;
import java.util.*;
public interface ExecucoesWorkflowRepository {
    List<ModeloWorkflow> modelos(UUID empresa,UUID tipo);
    Optional<ModeloWorkflow> modelo(UUID empresa,UUID id);
    boolean gatilhoAtivo(UUID empresa,UUID workflow,UUID tipo);
    Optional<ExecucaoWorkflow> porRequisicao(UUID empresa,UUID requisicao);
    void iniciar(UUID empresa,UUID requisicao,UUID autor,ExecucaoWorkflow execucao);
    void vincularEtapa(UUID empresa,UUID execucao,UUID atividade,EtapaPlanejada etapa);
}
