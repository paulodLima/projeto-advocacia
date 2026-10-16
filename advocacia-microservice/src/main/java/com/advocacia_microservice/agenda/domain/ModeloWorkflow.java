package com.advocacia_microservice.agenda.domain;
import java.util.*;
public record ModeloWorkflow(UUID id,String nome,int versao,int buffer,List<EtapaWorkflow> etapas) {
    public ModeloWorkflow {etapas=List.copyOf(etapas);}
}
