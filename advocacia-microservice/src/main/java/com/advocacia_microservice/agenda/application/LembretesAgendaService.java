package com.advocacia_microservice.agenda.application;

import com.advocacia_microservice.agenda.domain.*;
import com.advocacia_microservice.agenda.domain.repository.LembretesAgendaRepository;
import com.advocacia_microservice.agenda.application.command.LerLembrete;
import com.advocacia_microservice.equipe.application.EquipeService;
import com.advocacia_microservice.shared.exception.*;
import java.time.Instant;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class LembretesAgendaService {
    private final LembretesAgendaRepository repository;
    private final EquipeService equipe;
    public LembretesAgendaService(LembretesAgendaRepository repository,EquipeService equipe) {this.repository=repository;this.equipe=equipe;}
    private UUID empresa(UUID usuario) {
        var acesso=equipe.acesso(usuario);
        if(acesso.empresaId()==null || !acesso.modulos().contains("agenda") || !equipe.ativo(usuario))
            throw new AccessDeniedException("Você não tem acesso à Agenda deste escritório.");
        return acesso.empresaId();
    }
    public PainelLembretesAgenda listar(UUID usuario,int pagina,int tamanho) {
        if(pagina<0 || pagina>1000000 || tamanho<1 || tamanho>50) throw new IllegalArgumentException("Paginação inválida.");
        var empresa=empresa(usuario);var preferencias=repository.preferencia(empresa,usuario);var agora=Instant.now();
        var itens=preferencias.ativo()?repository.listar(empresa,usuario,preferencias.limite(agora),pagina,tamanho):new PaginaLembretes(List.of(),0,pagina,tamanho);
        return new PainelLembretesAgenda(preferencias,itens,agora);
    }
    @Transactional
    public PreferenciaLembrete salvarPreferencias(UUID usuario,PreferenciaLembrete preferencias) {
        var empresa=empresa(usuario);repository.bloquear(empresa);repository.salvarPreferencia(empresa,usuario,preferencias);return preferencias;
    }
    @Transactional
    public void ler(UUID usuario,UUID atividade,LerLembrete comando) {
        var empresa=empresa(usuario);repository.bloquear(empresa);
        if(comando.versao()<0 || comando.marco()==null) throw new IllegalArgumentException("Aviso inválido.");
        var preferencias=repository.preferencia(empresa,usuario);
        if(!preferencias.ativo()) throw new ConflitoException("Os lembretes estão desativados. Atualize o painel.");
        var lembrete=repository.buscar(empresa,usuario,preferencias.limite(Instant.now()),atividade,comando.versao(),comando.marco())
            .orElseThrow(()->new RecursoNaoEncontradoException("O aviso foi alterado ou não está mais disponível para você. Atualize o painel."));
        repository.ler(empresa,usuario,lembrete);
    }
}
