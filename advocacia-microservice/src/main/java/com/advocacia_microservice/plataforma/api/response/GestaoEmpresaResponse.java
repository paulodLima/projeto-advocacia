package com.advocacia_microservice.plataforma.api.response;
import com.advocacia_microservice.plataforma.domain.GestaoEmpresa;
import java.time.LocalDate;
import java.util.*;
public record GestaoEmpresaResponse(UUID id, String nome, String cnpj, String responsavel, String emailResponsavel,
        long usuariosAtivos, String situacao, String financeiro, LocalDate inicio, long versao,
        List<GestaoEmpresa.Modulo> modulos, List<GestaoEmpresa.Evento> historico) {
    public static GestaoEmpresaResponse de(GestaoEmpresa e) { return new GestaoEmpresaResponse(e.id(),e.nome(),e.cnpj(),e.responsavel(),e.emailResponsavel(),e.usuariosAtivos(),e.situacao(),e.financeiro(),e.inicio(),e.versao(),e.modulos(),e.historico()); }
}
