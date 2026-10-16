package com.advocacia_microservice.agenda.application;

import com.advocacia_microservice.agenda.domain.*;
import com.advocacia_microservice.agenda.domain.repository.AgendaRepository;
import com.advocacia_microservice.agenda.application.command.*;
import com.advocacia_microservice.equipe.application.EquipeService;
import com.advocacia_microservice.equipe.domain.PerfilEquipe;
import com.advocacia_microservice.empresa.domain.PapelEmpresa;
import com.advocacia_microservice.shared.exception.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class AgendaService {
    private final AgendaRepository repository;
    private final EquipeService equipe;
    public AgendaService(AgendaRepository repository,EquipeService equipe) { this.repository=repository; this.equipe=equipe; }
    private EquipeService.Acesso acesso(UUID usuario,boolean escrita) {
        var a=equipe.acesso(usuario);
        if (a.empresaId()==null || !a.modulos().contains("agenda") || !equipe.ativo(usuario)) throw new AccessDeniedException("Você não tem acesso à Agenda deste escritório.");
        if (escrita && !edita(a)) throw new AccessDeniedException("Seu perfil permite apenas consultar a Agenda.");
        return a;
    }
    private boolean edita(EquipeService.Acesso a) {
        return a.papel()==PapelEmpresa.MASTER || Set.of(PerfilEquipe.ADMINISTRADOR,PerfilEquipe.ADVOGADO,PerfilEquipe.ASSISTENTE).contains(a.perfil());
    }
    public OpcoesAgenda opcoes(UUID usuario) {
        var a=acesso(usuario,false);
        return new OpcoesAgenda(edita(a),a.modulos().contains("contatos"),repository.responsaveis(a.empresaId()),repository.tiposTarefa(a.empresaId()),"America/Sao_Paulo");
    }
    public PaginaAgenda listar(UUID usuario,FiltroAgenda filtro,int pagina,int tamanho) {
        if (pagina<0 || tamanho<1 || tamanho>100) throw new IllegalArgumentException("Paginação inválida.");
        var a=acesso(usuario,false);var resultado=repository.listar(a.empresaId(),filtro,pagina,tamanho);
        return new PaginaAgenda(resultado.itens().stream().map(item->visivel(a,item)).toList(),resultado.total(),resultado.pagina(),resultado.tamanho());
    }
    public List<DiaAgenda> calendario(UUID usuario,FiltroAgenda filtro) {
        if (ChronoUnit.DAYS.between(filtro.inicio(),filtro.fim())>61) throw new IllegalArgumentException("O calendário aceita até 62 dias por consulta.");
        var marcos=repository.marcos(acesso(usuario,false).empresaId(),filtro);
        var dias=new ArrayList<DiaAgenda>();
        for(var dia=filtro.inicio();!dia.isAfter(filtro.fim());dia=dia.plusDays(1)) {
            var data=dia; var ocorrencias=marcos.stream().filter(m->m.ocorreEm(data)).toList();
            dias.add(new DiaAgenda(dia,ocorrencias.size(),ocorrencias.stream().filter(m->m.situacao()==SituacaoAgenda.PENDENTE).count(),ocorrencias.stream().filter(m->data.equals(m.prazoFatal()) && m.situacao()==SituacaoAgenda.PENDENTE).count()));
        }
        return dias;
    }
    public AtividadeAgenda buscar(UUID usuario,UUID id) { var a=acesso(usuario,false); return visivel(a,exigir(a.empresaId(),id)); }
    public List<OpcaoAgenda> contatos(UUID usuario,String busca,List<UUID> ids) {
        var a=acesso(usuario,false);
        if (!a.modulos().contains("contatos")) throw new AccessDeniedException("Você não tem acesso aos Contatos.");
        if (busca==null || busca.length()>150 || ids.size()>50 || ids.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Busca de contatos inválida.");
        return repository.contatos(a.empresaId(),busca.strip(),ids);
    }
    private AtividadeAgenda visivel(EquipeService.Acesso acesso,AtividadeAgenda a) {
        if (acesso.modulos().contains("contatos")) return a;
        var d=a.dados();
        var dados=new DadosAtividade(d.tipo(),d.titulo(),d.tipoTarefaId(),d.data(),d.dataFim(),d.hora(),d.horaFim(),d.prazoFatal(),d.diaInteiro(),d.modalidade(),d.local(),d.descricao(),d.responsaveis(),List.of());
        return new AtividadeAgenda(a.id(),a.empresaId(),a.versao(),dados,a.situacao(),a.concluidaEm(),a.criadoEm(),a.atualizadoEm(),a.historico());
    }
    private AtividadeAgenda exigir(UUID empresa,UUID id) { return repository.buscar(empresa,id).orElseThrow(()->new RecursoNaoEncontradoException("Atividade não encontrada no seu escritório.")); }
    @Transactional
    public AtividadeAgenda salvar(UUID usuario,UUID id,SalvarAtividade c) {
        var acesso=acesso(usuario,true); var empresa=acesso.empresaId(); repository.bloquear(empresa);
        var anterior=id==null?null:exigir(empresa,id);
        if (c.versao()<0 || anterior!=null && anterior.versao()!=c.versao()) throw new ConflitoException("A atividade foi alterada em outra sessão. Recarregue a ficha antes de salvar.");
        if (anterior!=null && anterior.situacao()!=SituacaoAgenda.PENDENTE) throw new ConflitoException("Reabra a atividade antes de editar.");
        var titulo=c.titulo();
        if (c.tipoTarefaId()!=null) {
            var tipo=repository.tiposTarefa(empresa).stream().filter(t->t.id().equals(c.tipoTarefaId()) && t.ativo()).findFirst().orElseThrow(()->new IllegalArgumentException("Selecione um tipo de tarefa ativo do seu escritório."));
            if (titulo==null || titulo.isBlank()) titulo=tipo.nome();
        }
        var participantes=c.participantes()==null?List.<UUID>of():c.participantes();
        if (!acesso.modulos().contains("contatos")) {
            if (!participantes.isEmpty()) throw new AccessDeniedException("Você não tem acesso aos Contatos.");
            participantes=anterior==null?List.of():anterior.dados().participantes();
        } else {
            // Validate cardinality before using the IDs in a bounded query.
            if (participantes.size()>50 || participantes.stream().anyMatch(Objects::isNull) || new HashSet<>(participantes).size()!=participantes.size())
                throw new IllegalArgumentException("Selecione até 50 participantes sem duplicidade.");
            if (!participantes.isEmpty() && repository.contatos(empresa,"",participantes).size()!=participantes.size())
                throw new IllegalArgumentException("Selecione contatos do seu escritório.");
        }
        var dados=new DadosAtividade(c.tipo(),titulo,c.tipoTarefaId(),c.data(),c.dataFim(),c.hora(),c.horaFim(),c.prazoFatal(),c.diaInteiro(),c.modalidade(),c.local(),c.descricao(),c.responsaveis(),participantes);
        var ativos=repository.responsaveis(empresa).stream().filter(OpcaoAgenda::ativo).map(OpcaoAgenda::id).toList();
        if (!ativos.containsAll(dados.responsaveis())) throw new IllegalArgumentException("Selecione responsáveis ativos do seu escritório.");
        var agora=Instant.now();
        var atividade=new AtividadeAgenda(id==null?UUID.randomUUID():id,empresa,anterior==null?0:anterior.versao()+1,dados,SituacaoAgenda.PENDENTE,null,anterior==null?agora:anterior.criadoEm(),agora,List.of());
        repository.salvar(atividade,anterior==null);
        repository.registrar(empresa,atividade.id(),usuario,anterior==null?"CRIADA":"EDITADA","");
        return visivel(acesso,exigir(empresa,atividade.id()));
    }
    @Transactional
    public AtividadeAgenda alterarSituacao(UUID usuario,UUID id,AlterarSituacaoAgenda c) {
        var empresa=acesso(usuario,true).empresaId();repository.bloquear(empresa);var atual=exigir(empresa,id);
        if (c.versao()!=atual.versao()) throw new ConflitoException("A atividade foi alterada em outra sessão. Recarregue a ficha.");
        var observacao=c.observacao()==null?"":c.observacao().strip();
        if (c.situacao()==null || observacao.length()>2000 || c.situacao()==SituacaoAgenda.CANCELADA && observacao.isBlank()) throw new IllegalArgumentException("Informe a situação e, para cancelar, um motivo de até 2000 caracteres.");
        if (atual.situacao()==c.situacao() || atual.situacao()!=SituacaoAgenda.PENDENTE && c.situacao()!=SituacaoAgenda.PENDENTE)
            throw new ConflitoException("Reabra a atividade antes de alterar sua situação.");
        var agora=Instant.now();
        repository.salvar(new AtividadeAgenda(id,empresa,atual.versao()+1,atual.dados(),c.situacao(),c.situacao()==SituacaoAgenda.CONCLUIDA?agora:null,atual.criadoEm(),agora,List.of()),false);
        repository.registrar(empresa,id,usuario,switch(c.situacao()){case PENDENTE->"REABERTA";case CONCLUIDA->"CONCLUIDA";case CANCELADA->"CANCELADA";},observacao);
        return buscar(usuario,id);
    }
}
