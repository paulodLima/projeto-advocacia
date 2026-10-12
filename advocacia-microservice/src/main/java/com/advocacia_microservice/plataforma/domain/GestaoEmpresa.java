package com.advocacia_microservice.plataforma.domain;

import java.time.*;
import java.util.*;

public record GestaoEmpresa(UUID id, String nome, String cnpj, String responsavel, String emailResponsavel,
        long usuariosAtivos, String situacao, String financeiro, LocalDate inicio, long versao,
        List<Modulo> modulos, List<Evento> historico) {
    public record Modulo(String codigo, String nome, String situacao, boolean liberado) {}
    public record Evento(UUID id, String autor, String acao, String anterior, String atual, String motivo, Instant criadoEm) {}
}
