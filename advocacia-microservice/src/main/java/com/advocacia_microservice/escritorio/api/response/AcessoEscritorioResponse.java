package com.advocacia_microservice.escritorio.api.response;
import com.advocacia_microservice.escritorio.domain.AcessoEscritorio;
import java.util.UUID;
public record AcessoEscritorioResponse(UUID id, String nome, String perfil) {
    public static AcessoEscritorioResponse de(AcessoEscritorio a) { return new AcessoEscritorioResponse(a.id(), a.nome(), a.perfil()); }
}
