package com.advocacia_microservice.empresa.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public record Empresa(UUID id, Map<String, String> dados) {
    public Empresa {
        Objects.requireNonNull(id);
        Objects.requireNonNull(dados);
        if (dados.keySet().stream().anyMatch(chave -> !CampoEmpresa.conhecido(chave))) {
            throw new IllegalArgumentException("Campo de empresa desconhecido.");
        }
        var normalizados = new LinkedHashMap<String, String>();
        for (var campo : CampoEmpresa.values()) {
            var valor = dados.getOrDefault(campo.chave(), "");
            if (valor == null) throw new IllegalArgumentException("Campo inválido: " + campo.chave());
            valor = valor.strip();
            if (campo.obrigatorio && valor.isEmpty()) throw new IllegalArgumentException("Preencha o campo " + campo.chave());
            if (valor.length() > campo.limite) throw new IllegalArgumentException("Limite excedido no campo " + campo.chave());
            normalizados.put(campo.chave(), valor);
        }
        var cnpj = normalizados.get("cnpj").replaceAll("[./\\-\\s]", "").toUpperCase(Locale.ROOT);
        if (cnpj.isBlank()) throw new IllegalArgumentException("CNPJ é obrigatório.");
        normalizados.put("cnpj", cnpj);
        var email = normalizados.get("email").toLowerCase(Locale.ROOT);
        if (!email.isEmpty() && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("E-mail da empresa inválido.");
        normalizados.put("email", email);
        var uf = normalizados.get("uf").toUpperCase(Locale.ROOT);
        if (!uf.isEmpty() && !uf.matches("[A-Z]{2}")) throw new IllegalArgumentException("UF deve ter duas letras.");
        normalizados.put("uf", uf);
        var data = normalizados.get("constituida_em");
        if (!data.isEmpty()) {
            try { LocalDate.parse(data); }
            catch (java.time.format.DateTimeParseException erro) { throw new IllegalArgumentException("Data de constituição inválida."); }
        }
        var aliquota = normalizados.get("iss_aliquota");
        if (!aliquota.isEmpty()) {
            try {
                var valor = new BigDecimal(aliquota.replace(',', '.'));
                if (valor.signum() < 0 || valor.compareTo(BigDecimal.valueOf(100)) > 0) throw new NumberFormatException();
            } catch (NumberFormatException erro) { throw new IllegalArgumentException("Alíquota de ISS deve estar entre 0 e 100."); }
        }
        dados = Map.copyOf(normalizados);
    }
}
