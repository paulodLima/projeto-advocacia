package com.advocacia_microservice.empresa.api.request;

import com.advocacia_microservice.empresa.application.EmpresaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public record EmpresaRequest(@NotNull @Size(max = 24)
            Map<@NotNull @Size(max = 40) String, @NotNull @Size(max = 1000) String> dados) {}
