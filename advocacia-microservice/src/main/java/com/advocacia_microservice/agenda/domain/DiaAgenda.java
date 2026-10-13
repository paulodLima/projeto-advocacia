package com.advocacia_microservice.agenda.domain;
import java.time.LocalDate;
public record DiaAgenda(LocalDate data, long total, long pendentes, long prazosFatais) {}
