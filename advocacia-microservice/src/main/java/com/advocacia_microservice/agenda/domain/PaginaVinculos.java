package com.advocacia_microservice.agenda.domain;
import java.util.List;
public record PaginaVinculos(List<VinculoAgenda> itens,long total,int pagina,int tamanho) {}
