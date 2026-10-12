package com.advocacia_microservice.documento.api.request;

import com.advocacia_microservice.documento.application.DocumentosConfigService;
import com.advocacia_microservice.documento.domain.PapelTimbrado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public record ModelosRequest(@NotNull @Size(max=9) Map<@NotNull @Size(max=50) String, @NotNull @Size(max=20000) String> modelos) {}
