package com.advocacia_microservice.agenda.api.request;
import com.advocacia_microservice.agenda.application.command.PlanejarWorkflow;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
public record PlanejarWorkflowRequest(@NotNull UUID workflowId,@NotNull @Min(0) Integer versao,@NotNull UUID tipoTarefaId,@NotNull LocalDate prazoFatal,@Size(max=366) List<@NotNull LocalDate> diasNaoUteis) {
    public PlanejarWorkflow comando(){return new PlanejarWorkflow(workflowId,versao,tipoTarefaId,prazoFatal,diasNaoUteis);}
}
