package com.advocacia_microservice.cadastro.api.request;

import com.advocacia_microservice.cadastro.application.CadastrosService;
import com.advocacia_microservice.cadastro.domain.*;
import com.advocacia_microservice.cadastro.infrastructure.CadastrosRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public record RegistroRequest(@NotNull @Size(max = 4) Map<@NotNull @Size(max = 30) String, @NotNull @Size(max = 150) String> campos, @NotNull Boolean ativo) {}
