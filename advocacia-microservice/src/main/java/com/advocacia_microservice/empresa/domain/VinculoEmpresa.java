package com.advocacia_microservice.empresa.domain;

import java.util.UUID;

public record VinculoEmpresa(UUID usuarioId, UUID empresaId, PapelEmpresa papel) {}
