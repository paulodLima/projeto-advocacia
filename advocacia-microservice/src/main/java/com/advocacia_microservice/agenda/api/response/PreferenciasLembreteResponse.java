package com.advocacia_microservice.agenda.api.response;
import com.advocacia_microservice.agenda.domain.PreferenciaLembrete;
public record PreferenciasLembreteResponse(boolean ativo,int antecedenciaMinutos) {
    public static PreferenciasLembreteResponse de(PreferenciaLembrete p) {return new PreferenciasLembreteResponse(p.ativo(),p.antecedenciaMinutos());}
}
