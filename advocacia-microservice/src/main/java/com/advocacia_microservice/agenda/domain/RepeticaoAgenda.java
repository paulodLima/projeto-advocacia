package com.advocacia_microservice.agenda.domain;
import java.time.LocalDate;
public enum RepeticaoAgenda {
    NUNCA, DIARIA, SEMANAL, MENSAL, ANUAL;
    public LocalDate data(LocalDate base,int indice) {
        if (base==null || indice<1 || this==NUNCA) throw new IllegalArgumentException("Recorrência inválida.");
        try {
            var data=switch(this) {
                case DIARIA -> base.plusDays(indice);
                case SEMANAL -> base.plusWeeks(indice);
                case MENSAL -> base.plusMonths(indice);
                case ANUAL -> base.plusYears(indice);
                default -> throw new IllegalArgumentException("Recorrência inválida.");
            };
            if(data.getYear()>9999) throw new IllegalArgumentException("A próxima ocorrência ultrapassa o ano 9999.");
            return data;
        } catch(java.time.DateTimeException | ArithmeticException e) {
            throw new IllegalArgumentException("A próxima ocorrência está fora do intervalo de datas permitido.");
        }
    }
}
