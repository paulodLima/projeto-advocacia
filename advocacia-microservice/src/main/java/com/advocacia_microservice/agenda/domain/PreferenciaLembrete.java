package com.advocacia_microservice.agenda.domain;
import java.time.*;
import java.util.Set;
public record PreferenciaLembrete(boolean ativo,int antecedenciaMinutos) {
    public static final ZoneId FUSO=ZoneId.of("America/Sao_Paulo");
    public static PreferenciaLembrete padrao() {return new PreferenciaLembrete(true,1440);}
    public PreferenciaLembrete {
        if(!Set.of(0,15,60,1440).contains(antecedenciaMinutos))
            throw new IllegalArgumentException("Escolha no horário, 15 minutos, 1 hora ou 1 dia antes.");
    }
    public LocalDateTime limite(Instant agora) {return LocalDateTime.ofInstant(agora,FUSO).plusMinutes(antecedenciaMinutos);}
}
