package com.advocacia_microservice.agenda.api.response;
import com.advocacia_microservice.agenda.application.PainelLembretesAgenda;
import java.time.Instant;
import java.util.List;
public record PainelLembretesResponse(PreferenciasLembreteResponse preferencias,List<LembreteAgendaResponse> itens,long total,int pagina,int tamanho,Instant consultadoEm,String fusoHorario) {
    public static PainelLembretesResponse de(PainelLembretesAgenda p) {
        var lista=p.pagina();return new PainelLembretesResponse(PreferenciasLembreteResponse.de(p.preferencias()),lista.itens().stream().map(LembreteAgendaResponse::de).toList(),lista.total(),lista.pagina(),lista.tamanho(),p.consultadoEm(),"America/Sao_Paulo");
    }
}
