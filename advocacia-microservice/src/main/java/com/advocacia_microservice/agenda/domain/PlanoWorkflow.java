package com.advocacia_microservice.agenda.domain;

import java.time.*;
import java.util.*;

public record PlanoWorkflow(UUID workflowId,String nome,int versao,UUID tipoTarefaId,LocalDate prazoFatal,int buffer,List<LocalDate> diasNaoUteis,List<EtapaPlanejada> etapas) {
    public PlanoWorkflow {diasNaoUteis=List.copyOf(diasNaoUteis);etapas=List.copyOf(etapas);}
    public static PlanoWorkflow calcular(ModeloWorkflow modelo,UUID tipo,LocalDate prazo,List<LocalDate> exclusoes) {
        if(modelo.etapas().isEmpty() || modelo.etapas().size()>100) throw new IllegalArgumentException("O workflow precisa ter de 1 a 100 etapas.");
        var calendario=new HashSet<>(exclusoes);var planejadas=new ArrayList<EtapaPlanejada>();
        var cursor=anterior(prazo,modelo.buffer(),calendario);
        for(int indice=modelo.etapas().size()-1;indice>=0;indice--) {
            var etapa=modelo.etapas().get(indice);
            var fim=cursor;var inicio=anterior(fim,etapa.dias()-1,calendario);
            planejadas.add(new EtapaPlanejada(etapa.id(),indice+1,etapa.nome(),etapa.responsavel(),etapa.responsavelNome(),etapa.dias(),inicio,fim));
            if(indice>0)cursor=anterior(inicio,1,calendario);
        }
        Collections.reverse(planejadas);
        return new PlanoWorkflow(modelo.id(),modelo.nome(),modelo.versao(),tipo,prazo,modelo.buffer(),exclusoes,planejadas);
    }
    public String assinatura() {
        try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private static boolean util(LocalDate data,Set<LocalDate> exclusoes) {
        return data.getDayOfWeek()!=DayOfWeek.SATURDAY && data.getDayOfWeek()!=DayOfWeek.SUNDAY && !exclusoes.contains(data);
    }
    private static LocalDate anterior(LocalDate data,int dias,Set<LocalDate> exclusoes) {
        if(dias<0 || dias>365)throw new IllegalArgumentException("Quantidade de dias de trabalho inválida.");
        try {
            if(dias==0) {while(!util(data,exclusoes))data=data.minusDays(1);}
            for(int n=0;n<dias;n++) {do{data=data.minusDays(1);}while(!util(data,exclusoes));}
            if(data.getYear()<1 || data.getYear()>9999)throw new IllegalArgumentException("Planejamento fora do intervalo de datas permitido.");
            return data;
        }catch(DateTimeException e){throw new IllegalArgumentException("Planejamento fora do intervalo de datas permitido.");}
    }
}
