package com.advocacia_microservice.agenda.application;

import com.advocacia_microservice.agenda.domain.*;
import com.advocacia_microservice.agenda.domain.repository.*;
import com.advocacia_microservice.agenda.application.command.PlanejarWorkflow;
import com.advocacia_microservice.equipe.application.EquipeService;
import com.advocacia_microservice.equipe.domain.PerfilEquipe;
import com.advocacia_microservice.empresa.domain.PapelEmpresa;
import com.advocacia_microservice.shared.exception.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class ExecucoesWorkflowService {
    private final ExecucoesWorkflowRepository repository;
    private final AgendaRepository agenda;
    private final EquipeService equipe;
    public ExecucoesWorkflowService(ExecucoesWorkflowRepository repository,AgendaRepository agenda,EquipeService equipe) {this.repository=repository;this.agenda=agenda;this.equipe=equipe;}
    private UUID empresa(UUID usuario) {
        var a=equipe.acesso(usuario);
        if(a.empresaId()==null || !a.modulos().contains("agenda") || !equipe.ativo(usuario))
            throw new AccessDeniedException("Você não tem acesso à Agenda deste escritório.");
        if(a.papel()!=PapelEmpresa.MASTER && !Set.of(PerfilEquipe.ADMINISTRADOR,PerfilEquipe.ADVOGADO,PerfilEquipe.ASSISTENTE).contains(a.perfil()))
            throw new AccessDeniedException("Seu perfil permite apenas consultar a Agenda.");
        return a.empresaId();
    }
    public List<ModeloWorkflow> modelos(UUID usuario,UUID tipo) {
        if(tipo==null)throw new IllegalArgumentException("Selecione um tipo de tarefa.");
        return repository.modelos(empresa(usuario),tipo);
    }
    public PlanoWorkflow prever(UUID usuario,PlanejarWorkflow comando) {return planejar(empresa(usuario),comando);}
    private PlanoWorkflow planejar(UUID empresa,PlanejarWorkflow c) {
        var modelo=repository.modelo(empresa,c.workflowId()).orElseThrow(()->new RecursoNaoEncontradoException("Workflow não encontrado no seu escritório."));
        if(modelo.versao()!=c.versao())throw new ConflitoException("O workflow foi alterado. Recarregue os modelos e confira uma nova prévia.");
        if(!repository.gatilhoAtivo(empresa,modelo.id(),c.tipoTarefaId()))throw new ConflitoException("O tipo de tarefa não está ativo ou não aciona mais este workflow.");
        var ativos=agenda.responsaveis(empresa).stream().filter(OpcaoAgenda::ativo).map(OpcaoAgenda::id).toList();
        if(modelo.etapas().stream().anyMatch(e->e.responsavel()!=null && !ativos.contains(e.responsavel())))
            throw new ConflitoException("O workflow possui responsável inativo ou fora do escritório. Corrija a configuração antes de gerar.");
        return PlanoWorkflow.calcular(modelo,c.tipoTarefaId(),c.prazoFatal(),c.diasNaoUteis());
    }
    @Transactional
    public ExecucaoWorkflow gerar(UUID usuario,UUID requisicao,PlanejarWorkflow comando,String assinaturaPlano) {
        if(assinaturaPlano==null || !assinaturaPlano.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Confira uma prévia antes de gerar.");
        if(requisicao==null)throw new IllegalArgumentException("Informe a identificação da requisição.");
        var empresa=empresa(usuario);agenda.bloquear(empresa);
        var assinatura=assinatura(comando,assinaturaPlano);var existente=repository.porRequisicao(empresa,requisicao);
        if(existente.isPresent()) {
            if(!existente.get().assinatura().equals(assinatura))throw new ConflitoException("Esta requisição já foi usada com outros dados. Confira uma nova prévia.");
            return existente.get();
        }
        var plano=planejar(empresa,comando);
        if(!plano.assinatura().equals(assinaturaPlano))throw new ConflitoException("O planejamento mudou. Confira uma nova prévia antes de gerar.");
        var ids=plano.etapas().stream().map(e->UUID.randomUUID()).toList();
        var execucao=new ExecucaoWorkflow(UUID.randomUUID(),assinatura,plano,ids);
        repository.iniciar(empresa,requisicao,usuario,execucao);var agora=Instant.now();
        for(int i=0;i<plano.etapas().size();i++) {
            var etapa=plano.etapas().get(i);var responsaveis=etapa.responsavel()==null?List.<UUID>of():List.of(etapa.responsavel());
            var dados=new DadosAtividade(TipoAtividade.TAREFA,etapa.nome(),null,etapa.inicio(),etapa.inicio().equals(etapa.fim())?null:etapa.fim(),null,null,null,true,null,"","",responsaveis);
            var atividade=new AtividadeAgenda(ids.get(i),empresa,0,dados,SituacaoAgenda.PENDENTE,null,agora,agora,List.of());
            agenda.salvar(atividade,true);repository.vincularEtapa(empresa,execucao.id(),atividade.id(),etapa);
            agenda.registrar(empresa,atividade.id(),usuario,"CRIADA","Etapa "+etapa.ordem()+" de "+plano.etapas().size()+" do workflow "+plano.nome()+". Planejamento interno de "+etapa.inicio()+" a "+etapa.fim()+".");
        }
        return execucao;
    }
    private static String assinatura(PlanejarWorkflow c,String plano) {
        var valor=c.workflowId()+"|"+c.versao()+"|"+c.tipoTarefaId()+"|"+c.prazoFatal()+"|"+c.diasNaoUteis()+"|"+plano;
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
