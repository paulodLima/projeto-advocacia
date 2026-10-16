package com.advocacia_microservice.agenda.domain;
import java.util.List;
public record PaginaLembretes(List<LembreteAgenda> itens,long total,int pagina,int tamanho) {}
