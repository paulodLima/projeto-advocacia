package com.advocacia_microservice.agenda.domain.repository;
import com.advocacia_microservice.agenda.domain.*;

import java.util.List;
public interface VinculosAgendaRepository {
 void sincronizarConsulta(java.util.UUID empresa,java.util.UUID lead);
 void sincronizarTarefa(java.util.UUID empresa,java.util.UUID tarefa);
 void reindexar();
 PaginaVinculos listar(AcessoVinculos acesso,FiltroAgenda filtro,int pagina,int tamanho);
 List<MarcoAgenda> marcos(AcessoVinculos acesso,FiltroAgenda filtro);
}
