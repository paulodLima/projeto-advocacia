package com.advocacia_microservice.agenda.domain;

import java.net.URI;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public record DadosAtividade(TipoAtividade tipo, String titulo, UUID tipoTarefaId, LocalDate data, LocalDate dataFim,
        LocalTime hora, LocalTime horaFim, LocalDate prazoFatal, boolean diaInteiro, String modalidade,
        String local, String descricao, List<UUID> responsaveis, List<UUID> participantes, RepeticaoAgenda repeticao) {
    public DadosAtividade(TipoAtividade tipo,String titulo,UUID tipoTarefaId,LocalDate data,LocalDate dataFim,LocalTime hora,LocalTime horaFim,LocalDate prazoFatal,boolean diaInteiro,String modalidade,String local,String descricao,List<UUID> responsaveis,List<UUID> participantes) {
        this(tipo,titulo,tipoTarefaId,data,dataFim,hora,horaFim,prazoFatal,diaInteiro,modalidade,local,descricao,responsaveis,participantes,RepeticaoAgenda.NUNCA);
    }
    public DadosAtividade(TipoAtividade tipo,String titulo,UUID tipoTarefaId,LocalDate data,LocalDate dataFim,LocalTime hora,LocalTime horaFim,LocalDate prazoFatal,boolean diaInteiro,String modalidade,String local,String descricao,List<UUID> responsaveis) {
        this(tipo,titulo,tipoTarefaId,data,dataFim,hora,horaFim,prazoFatal,diaInteiro,modalidade,local,descricao,responsaveis,List.of());
    }
    public DadosAtividade {
        repeticao=repeticao==null?RepeticaoAgenda.NUNCA:repeticao;
        if (repeticao!=RepeticaoAgenda.NUNCA && (tipo!=TipoAtividade.TAREFA || data==null))
            throw new IllegalArgumentException("A repetição exige uma tarefa com data inicial.");
        if (tipo==null) throw new IllegalArgumentException("Selecione o tipo de atividade.");
        titulo = texto(titulo,150,"título"); local = texto(local,1000,"local/link"); descricao = texto(descricao,4000,"descrição");
        if (titulo.isBlank()) throw new IllegalArgumentException("Informe um título ou selecione um tipo de tarefa.");
        if (data==null && prazoFatal!=null) data=prazoFatal;
        if (data==null && tipo!=TipoAtividade.TAREFA) throw new IllegalArgumentException("Informe a data do compromisso.");
        if (dataFim!=null && (data==null || dataFim.isBefore(data) || ChronoUnit.DAYS.between(data,dataFim)>365))
            throw new IllegalArgumentException("A data final deve estar entre a data inicial e 365 dias depois.");
        if (tipo==TipoAtividade.PRAZO && prazoFatal==null) throw new IllegalArgumentException("Informe o prazo fatal manualmente.");
        if (prazoFatal!=null && (tipo==TipoAtividade.REUNIAO || tipo==TipoAtividade.AUDIENCIA || prazoFatal.isBefore(dataFim==null?data:dataFim)))
            throw new IllegalArgumentException("O prazo fatal é permitido para tarefas/prazos e não pode preceder a data de execução.");
        if (diaInteiro) { hora=null; horaFim=null; }
        if (data==null && (hora!=null || horaFim!=null || dataFim!=null)) throw new IllegalArgumentException("Informe uma data antes de definir horários.");
        if (horaFim!=null && hora==null) throw new IllegalArgumentException("Informe o horário inicial.");
        if (hora!=null && (hora.getSecond()!=0 || hora.getNano()!=0) || horaFim!=null && (horaFim.getSecond()!=0 || horaFim.getNano()!=0))
            throw new IllegalArgumentException("Use horários com precisão de minutos.");
        if (horaFim!=null && (dataFim==null || dataFim.equals(data)) && !horaFim.isAfter(hora))
            throw new IllegalArgumentException("O horário final deve ser posterior ao inicial.");
        boolean compromisso=tipo==TipoAtividade.REUNIAO || tipo==TipoAtividade.AUDIENCIA;
        if (compromisso && !diaInteiro && (hora==null || horaFim==null)) throw new IllegalArgumentException("Informe início e fim ou marque dia inteiro.");
        if (compromisso) {
            if (!Set.of("PRESENCIAL","ONLINE").contains(modalidade==null?"":modalidade)) throw new IllegalArgumentException("Selecione presencial ou online.");
            if (modalidade.equals("ONLINE") && !local.isBlank()) {
                try { var uri=URI.create(local); if (!Set.of("http","https").contains(Objects.toString(uri.getScheme(),"")) || uri.getHost()==null || uri.getUserInfo()!=null) throw new IllegalArgumentException(); }
                catch (IllegalArgumentException e) { throw new IllegalArgumentException("Informe um link HTTP/HTTPS sem credenciais."); }
            }
        } else modalidade=null;
        if (responsaveis==null || responsaveis.size()>20 || responsaveis.stream().anyMatch(Objects::isNull) || new HashSet<>(responsaveis).size()!=responsaveis.size())
            throw new IllegalArgumentException("Selecione até 20 responsáveis sem duplicidade.");
        responsaveis=List.copyOf(responsaveis);
        participantes=participantes==null?List.of():participantes;
        if (participantes.size()>50 || participantes.stream().anyMatch(Objects::isNull) || new HashSet<>(participantes).size()!=participantes.size())
            throw new IllegalArgumentException("Selecione até 50 participantes sem duplicidade.");
        participantes=List.copyOf(participantes);
    }
    private static String texto(String valor,int limite,String campo) {
        var texto=valor==null?"":valor.strip();
        if (texto.length()>limite) throw new IllegalArgumentException("O campo "+campo+" aceita até "+limite+" caracteres.");
        return texto;
    }
}
