package com.advocacia_microservice.agenda.api.response;
import com.advocacia_microservice.agenda.domain.EtapaPlanejada;
import java.time.LocalDate;
import java.util.UUID;
public record EtapaPlanejadaResponse(int ordem,String nome,UUID responsavel,String responsavelNome,int dias,LocalDate inicio,LocalDate fim) {
    public static EtapaPlanejadaResponse de(EtapaPlanejada e){return new EtapaPlanejadaResponse(e.ordem(),e.nome(),e.responsavel(),e.responsavelNome(),e.dias(),e.inicio(),e.fim());}
}
