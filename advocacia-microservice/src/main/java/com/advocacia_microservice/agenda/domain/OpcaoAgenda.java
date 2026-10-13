package com.advocacia_microservice.agenda.domain;
import java.util.UUID;
public record OpcaoAgenda(UUID id, String nome, boolean ativo) {}
