package com.advocacia_microservice.workflow.api.request;

import com.advocacia_microservice.workflow.application.WorkflowsService;
import com.advocacia_microservice.workflow.application.WorkflowsService.Etapa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public record Criar(@NotBlank @Size(max=150) String nome) {}
