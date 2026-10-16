package com.advocacia_microservice.agenda.domain;
import java.time.LocalDate;
import java.util.UUID;
public record EtapaPlanejada(UUID id,int ordem,String nome,UUID responsavel,String responsavelNome,int dias,LocalDate inicio,LocalDate fim) {}
