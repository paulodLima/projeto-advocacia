package com.advocacia_microservice.agenda.domain.repository;
import com.advocacia_microservice.agenda.domain.*;
import java.time.LocalDateTime;
import java.util.*;
public interface LembretesAgendaRepository {
    void bloquear(UUID empresa);
    PreferenciaLembrete preferencia(UUID empresa,UUID usuario);
    void salvarPreferencia(UUID empresa,UUID usuario,PreferenciaLembrete preferencia);
    PaginaLembretes listar(UUID empresa,UUID usuario,LocalDateTime limite,int pagina,int tamanho);
    Optional<LembreteAgenda> buscar(UUID empresa,UUID usuario,LocalDateTime limite,UUID atividade,long versao,MarcoLembrete marco);
    void ler(UUID empresa,UUID usuario,LembreteAgenda lembrete);
}
