package com.advocacia_microservice.agenda.api.response;
import com.advocacia_microservice.agenda.domain.PaginaAgenda;
import java.util.List;
public record PaginaAgendaResponse(List<AtividadeResponse> itens,long total,int pagina,int tamanho) {
    public static PaginaAgendaResponse de(PaginaAgenda p) {return new PaginaAgendaResponse(p.itens().stream().map(AtividadeResponse::de).toList(),p.total(),p.pagina(),p.tamanho());}
}
