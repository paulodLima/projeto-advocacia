package com.advocacia_microservice.agenda.application;
import com.advocacia_microservice.agenda.domain.*;
import java.time.Instant;
public record PainelLembretesAgenda(PreferenciaLembrete preferencias,PaginaLembretes pagina,Instant consultadoEm) {}
