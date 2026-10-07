package com.advocacia_microservice.documento.domain;
import com.advocacia_microservice.empresa.domain.IdentidadeVisual;
import java.util.*;

public record PapelTimbrado(String logo, String rodape, String alinhamento, boolean repetir, boolean documento, boolean endereco, boolean contato) {
    public PapelTimbrado {
        if (logo == null || rodape == null || alinhamento == null || !Set.of("esquerda", "centro", "direita").contains(alinhamento)) throw new IllegalArgumentException("Verifique as imagens e o alinhamento do papel timbrado.");
        if (logo.length() > 2796240 || rodape.length() > 2796240) throw new IllegalArgumentException("Cada imagem deve ter até 2 MB.");
    }
    public static PapelTimbrado padrao() { return new PapelTimbrado("", "", "centro", false, true, true, true); }
    public PapelTimbrado normalizado() {
        var imagens = new HashMap<String, String>();
        if (!logo.isEmpty()) imagens.put("logo_completa_marrom", logo);
        if (!rodape.isEmpty()) imagens.put("foto_login", rodape);
        var validas = new IdentidadeVisual(imagens, Map.of(), 1, 50, 50).normalizarImagens().imagens();
        return new PapelTimbrado(validas.getOrDefault("logo_completa_marrom", ""), validas.getOrDefault("foto_login", ""), alinhamento, repetir, documento, endereco, contato);
    }
}
