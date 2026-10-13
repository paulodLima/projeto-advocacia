package com.advocacia_microservice.agenda.domain;
import java.time.Instant;
import java.util.UUID;
public record EventoAgenda(UUID id, String autor, String acao, String observacao, Instant criadoEm) {}
