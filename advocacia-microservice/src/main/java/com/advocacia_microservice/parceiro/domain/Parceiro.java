package com.advocacia_microservice.parceiro.domain;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.*;
import javax.imageio.ImageIO;

public record Parceiro(UUID id, UUID empresaId, int versao, Map<String, String> dados,
                       List<Socio> socios, List<String> areasAtuacao) {
    public record Socio(String nome, String oab) {
        public Socio {
            nome = texto(nome, 150);
            oab = texto(oab, 150);
            if (nome.isEmpty()) throw new IllegalArgumentException("Informe o nome do sócio.");
        }
    }
    public static final List<String> AREAS = List.of("Família", "Sucessões", "Agronegócio", "Consumidor",
            "Cível", "Trabalhista", "Tributário", "Empresarial", "Criminal", "Previdenciário", "Imobiliário", "Ambiental");
    public static final Set<String> COMUNS = Set.of("nome", "tipo_pessoa", "nome_fantasia", "documento", "email",
            "telefone", "cep", "logradouro", "numero", "complemento", "bairro", "cidade", "uf", "observacoes");
    public static final Set<String> EXTRAS = Set.of("oab", "advogado_responsavel", "foto_propria", "site", "instagram");

    public Parceiro {
        if (dados == null || dados.keySet().stream().anyMatch(k -> !COMUNS.contains(k) && !EXTRAS.contains(k)))
            throw new IllegalArgumentException("Campos de parceiro inválidos.");
        var copia = new HashMap<String, String>();
        for (var campo : COMUNS) copia.put(campo, texto(dados.getOrDefault(campo, ""), campo.equals("observacoes") ? 4000 : campo.equals("email") ? 254 : 150));
        for (var campo : EXTRAS) copia.put(campo, texto(dados.getOrDefault(campo, ""), campo.equals("foto_propria") ? 700000 : campo.equals("site") ? 1000 : 150));
        validarFoto(copia.get("foto_propria"));
        if (!copia.get("site").isEmpty()) {
            try {
                var uri = URI.create(copia.get("site"));
                if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
                    throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) { throw new IllegalArgumentException("Informe um site HTTP ou HTTPS válido."); }
        }
        if (socios == null || socios.size() > 20 || socios.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Cadastre até 20 sócios.");
        socios = "PJ".equals(copia.get("tipo_pessoa")) ? List.copyOf(socios) : List.of();
        if ("PF".equals(copia.get("tipo_pessoa"))) copia.put("nome_fantasia", "");
        if (areasAtuacao == null || areasAtuacao.size() > AREAS.size() || areasAtuacao.stream().anyMatch(Objects::isNull) || !AREAS.containsAll(areasAtuacao)
                || new HashSet<>(areasAtuacao).size() != areasAtuacao.size())
            throw new IllegalArgumentException("Áreas de atuação inválidas.");
        areasAtuacao = List.copyOf(areasAtuacao);
        dados = Map.copyOf(copia);
    }

    private static String texto(String valor, int limite) {
        if (valor == null || valor.length() > limite) throw new IllegalArgumentException("Verifique o tamanho dos campos do parceiro.");
        return valor.strip();
    }

    private static void validarFoto(String foto) {
        if (foto.isEmpty()) return;
        if (!foto.matches("data:image/(jpeg|png);base64,[A-Za-z0-9+/=]+")) throw new IllegalArgumentException("Envie uma foto JPEG ou PNG.");
        try {
            var bytes = Base64.getDecoder().decode(foto.substring(foto.indexOf(',') + 1));
            if (bytes.length > 512 * 1024) throw new IllegalArgumentException();
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException();
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    var esperado = foto.startsWith("data:image/png;") ? "png" : "jpeg";
                    if (!reader.getFormatName().equalsIgnoreCase(esperado) || reader.getWidth(0) > 512 || reader.getHeight(0) > 512 || reader.read(0) == null)
                        throw new IllegalArgumentException();
                } finally { reader.dispose(); }
            }
        } catch (java.io.IOException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Foto inválida. Envie JPEG ou PNG de até 512 × 512 pixels e 512 KB.");
        }
    }
}
