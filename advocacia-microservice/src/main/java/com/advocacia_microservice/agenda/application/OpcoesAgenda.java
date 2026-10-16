package com.advocacia_microservice.agenda.application;
import com.advocacia_microservice.agenda.domain.OpcaoAgenda;
import java.util.List;
public record OpcoesAgenda(boolean podeEditar,boolean podeVincularContatos,List<OpcaoAgenda> responsaveis,List<OpcaoAgenda> tiposTarefa,String fusoHorario) {}
