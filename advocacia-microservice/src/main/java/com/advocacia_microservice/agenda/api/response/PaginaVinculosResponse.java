package com.advocacia_microservice.agenda.api.response;
import com.advocacia_microservice.agenda.domain.*;
import java.util.List;
public record PaginaVinculosResponse(List<VinculoAgenda> itens,long total,int pagina,int tamanho){
 public static PaginaVinculosResponse de(PaginaVinculos p){return new PaginaVinculosResponse(p.itens(),p.total(),p.pagina(),p.tamanho());}
}
