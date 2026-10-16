package com.advocacia_microservice.agenda.domain;
import java.util.UUID;
public record EtapaWorkflow(UUID id,String nome,UUID responsavel,String responsavelNome,int dias) {}
