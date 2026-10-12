package com.advocacia_microservice.workflow.api.request;

import com.advocacia_microservice.workflow.application.WorkflowsService;
import com.advocacia_microservice.workflow.application.WorkflowsService.Etapa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public record Salvar(@NotBlank @Size(max=150) String nome,@NotNull @Min(0) @Max(365) Integer buffer,@NotNull @Min(0) Integer versao,@NotNull @Size(max=100) List<@NotNull UUID> gatilhos,@NotNull @Size(max=100) List<@NotNull Etapa> etapas) {}
