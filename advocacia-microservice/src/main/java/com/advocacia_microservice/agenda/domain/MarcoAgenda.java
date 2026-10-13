package com.advocacia_microservice.agenda.domain;
import java.time.LocalDate;
public record MarcoAgenda(LocalDate data, LocalDate fim, LocalDate prazoFatal, SituacaoAgenda situacao) {
    public boolean ocorreEm(LocalDate dia) {
        return data!=null && !dia.isBefore(data) && !dia.isAfter(fim==null?data:fim) || dia.equals(prazoFatal);
    }
}
