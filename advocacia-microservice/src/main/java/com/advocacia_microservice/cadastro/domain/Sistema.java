package com.advocacia_microservice.cadastro.domain;
import java.net.URI;
import java.util.*;
import com.advocacia_microservice.empresa.domain.IdentidadeVisual;

public record Sistema(UUID id, String nome, String url, boolean ativo, String icone, String cor, String logo) {
    public Sistema {
        if (id == null || nome == null || nome.isBlank() || nome.strip().length() > 150 || url == null || url.length() > 1000)
            throw new IllegalArgumentException("Informe título e endereço válido para cada sistema.");
        nome = nome.strip(); url = url.strip();
        try {
            var endereco = URI.create(url);
            if (!("http".equalsIgnoreCase(endereco.getScheme()) || "https".equalsIgnoreCase(endereco.getScheme())) || endereco.getHost() == null || endereco.getRawUserInfo() != null)
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException erro) { throw new IllegalArgumentException("Use um endereço http:// ou https:// válido e sem credenciais."); }
        if (icone == null || !Set.of("link", "scale", "calendar", "file", "wallet").contains(icone)) throw new IllegalArgumentException("Ícone inválido.");
        if (cor == null || !cor.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Cor inválida.");
        logo = logo == null ? "" : logo;
        if (logo.length() > 350_000) throw new IllegalArgumentException("A logo do sistema deve ter até 256 KB.");
    }
    public Sistema normalizarLogo() {
        if (logo.isEmpty()) return this;
        try {
            if (Base64.getDecoder().decode(logo.substring(logo.indexOf(',') + 1)).length > 256 * 1024) throw new IllegalArgumentException("A logo do sistema deve ter até 256 KB.");
        } catch (IllegalArgumentException erro) { throw new IllegalArgumentException("A logo deve ser PNG ou JPEG válido de até 256 KB."); }
        var validada = new IdentidadeVisual(Map.of("foto_login", logo), Map.of(), 1, 50, 50).normalizarImagens().imagens().get("foto_login");
        return new Sistema(id, nome, url, ativo, icone, cor, validada);
    }
}
