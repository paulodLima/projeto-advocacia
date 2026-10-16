package com.advocacia_microservice.agenda.api.response;
import com.advocacia_microservice.agenda.application.OpcoesAgenda;
import com.advocacia_microservice.agenda.domain.OpcaoAgenda;
import java.util.List;
public record OpcoesAgendaResponse(boolean podeEditar,boolean podeVincularContatos,List<OpcaoAgenda> responsaveis,List<OpcaoAgenda> tiposTarefa,String fusoHorario) {
    public static OpcoesAgendaResponse de(OpcoesAgenda o) {return new OpcoesAgendaResponse(o.podeEditar(),o.podeVincularContatos(),o.responsaveis(),o.tiposTarefa(),o.fusoHorario());}
}
