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
    List<OpcaoAgenda> contatos(UUID empresa,String busca,List<UUID> ids);
    CicloRecorrencia ciclo(UUID empresa,UUID atividade);
    boolean temSucessora(UUID empresa,UUID atividade);
    void vincularSucessora(UUID empresa,UUID anterior,UUID proxima,CicloRecorrencia ciclo);
    void salvar(AtividadeAgenda atividade,boolean nova);
    void registrar(UUID empresa,UUID atividade,UUID autor,String acao,String observacao);
}
