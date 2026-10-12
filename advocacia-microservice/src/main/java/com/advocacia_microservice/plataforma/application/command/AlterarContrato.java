package com.advocacia_microservice.plataforma.application.command;
import java.time.LocalDate;
import java.util.Set;
public record AlterarContrato(long versao, String situacao, String financeiro, LocalDate inicio, Set<String> modulos, String motivo) {}
