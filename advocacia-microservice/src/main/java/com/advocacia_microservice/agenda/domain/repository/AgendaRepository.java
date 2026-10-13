package com.advocacia_microservice.agenda.domain.repository;
import com.advocacia_microservice.agenda.domain.*;
import java.util.*;
public interface AgendaRepository {
    void bloquear(UUID empresa);
    Optional<AtividadeAgenda> buscar(UUID empresa,UUID id);
    PaginaAgenda listar(UUID empresa,FiltroAgenda filtro,int pagina,int tamanho);
    List<MarcoAgenda> marcos(UUID empresa,FiltroAgenda filtro);
    List<OpcaoAgenda> responsaveis(UUID empresa);
    List<OpcaoAgenda> tiposTarefa(UUID empresa);
    void salvar(AtividadeAgenda atividade,boolean nova);
    void registrar(UUID empresa,UUID atividade,UUID autor,String acao,String observacao);
}
