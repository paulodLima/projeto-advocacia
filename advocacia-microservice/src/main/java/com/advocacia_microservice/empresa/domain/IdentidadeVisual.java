package com.advocacia_microservice.empresa.domain;

import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;

public record IdentidadeVisual(Map<String, String> imagens, Map<String, String> usos,
                              double zoom, double posX, double posY) {
    public static final Set<String> LOGOS = Set.of("logo_completa_branca", "logo_simples_branca", "logo_completa_marrom", "logo_simples_marrom");
    public static final Set<String> LOCAIS = Set.of("menu_expandido", "menu_recolhido", "login");
    public IdentidadeVisual {
        if (imagens == null || usos == null || imagens.size() > 5 || usos.size() > 3)
            throw new IllegalArgumentException("Informe as imagens e os locais da identidade visual.");
        if (!Double.isFinite(zoom) || zoom < 1 || zoom > 2.5 || !Double.isFinite(posX) || posX < 0 || posX > 100 || !Double.isFinite(posY) || posY < 0 || posY > 100)
            throw new IllegalArgumentException("Zoom ou posição da imagem inválidos.");
        var verificadas = new HashMap<String, String>();
        imagens.forEach((tipo, imagem) -> {
            if (!LOGOS.contains(tipo == null ? "" : tipo) && !"foto_login".equals(tipo)) throw new IllegalArgumentException("Tipo de imagem inválido.");
            if (imagem == null || imagem.length() > 2_796_240) throw new IllegalArgumentException("Cada imagem deve ter até 2 MB.");
            verificadas.put(tipo, imagem);
        });
        usos.forEach((local, tipo) -> {
            if (!LOCAIS.contains(local == null ? "" : local) || !LOGOS.contains(tipo == null ? "" : tipo) || !verificadas.containsKey(tipo))
                throw new IllegalArgumentException("Escolha uma logo enviada para cada local.");
        });
        imagens = Map.copyOf(verificadas); usos = Map.copyOf(usos);
    }
    public static IdentidadeVisual padrao() { return new IdentidadeVisual(Map.of(), Map.of(), 1, 50, 50); }
    public IdentidadeVisual normalizarImagens() {
        var normalizadas = new HashMap<String, String>();
        imagens.forEach((tipo, imagem) -> normalizadas.put(tipo, validarImagem(imagem)));
        return new IdentidadeVisual(normalizadas, usos, zoom, posX, posY);
    }
    private static String validarImagem(String valor) {
        if (valor == null || valor.length() > 2_796_240) throw new IllegalArgumentException("Cada imagem deve ter até 2 MB.");
        String formato = valor.startsWith("data:image/png;base64,") ? "png" : valor.startsWith("data:image/jpeg;base64,") ? "jpeg" : null;
        if (formato == null) throw new IllegalArgumentException("Envie uma imagem PNG ou JPEG.");
        try {
            byte[] bytes = Base64.getDecoder().decode(valor.substring(valor.indexOf(',') + 1));
            if (bytes.length > 2 * 1024 * 1024) throw new IllegalArgumentException("Cada imagem deve ter até 2 MB.");
            try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw new IllegalArgumentException("Arquivo de imagem inválido.");
                var reader = readers.next();
                try {
                    reader.setInput(stream);
                    if (!reader.getFormatName().equalsIgnoreCase(formato) || reader.getWidth(0) < 1 || reader.getHeight(0) < 1 || reader.getWidth(0) > 1400 || reader.getHeight(0) > 1400)
                        throw new IllegalArgumentException("A imagem deve ser PNG ou JPEG com até 1400 pixels por lado.");
                    // Reencode para armazenar apenas os pixels, sem metadados ou conteúdo anexado.
                    var saida = new ByteArrayOutputStream();
                    if (!ImageIO.write(reader.read(0), formato, saida) || saida.size() > 2 * 1024 * 1024)
                        throw new IllegalArgumentException("Cada imagem deve ter até 2 MB.");
                    return "data:image/" + formato + ";base64," + Base64.getEncoder().encodeToString(saida.toByteArray());
                } finally { reader.dispose(); }
            }
        } catch (IOException error) { throw new IllegalArgumentException("Arquivo de imagem inválido."); }
    }
}
