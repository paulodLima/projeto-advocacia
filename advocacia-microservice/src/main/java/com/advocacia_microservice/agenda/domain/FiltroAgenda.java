package com.advocacia_microservice.agenda.domain;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
public record FiltroAgenda(LocalDate inicio, LocalDate fim, TipoAtividade tipo, SituacaoAgenda situacao,
        UUID responsavel, String busca, boolean incluirSemData) {
    public FiltroAgenda {
        if (inicio==null || fim==null || fim.isBefore(inicio) || ChronoUnit.DAYS.between(inicio,fim)>366)
            throw new IllegalArgumentException("Selecione um período de até 366 dias.");
        busca=busca==null?"":busca.strip();
        if (busca.length()>150) throw new IllegalArgumentException("A busca aceita até 150 caracteres.");
    }
}
