package com.advocacia_microservice.agenda.domain;
import java.time.LocalDate;
import java.util.UUID;
public record OrigemWorkflow(UUID execucaoId,String nome,int ordem,int totalEtapas,LocalDate prazoFatal,LocalDate inicioPlanejado,LocalDate fimPlanejado) {}
