package com.advocacia_microservice.agenda.domain;
import java.util.List;
public record PaginaAgenda(List<AtividadeAgenda> itens,long total,int pagina,int tamanho) {}
