package com.advocacia_microservice.cadastro.domain;

import java.util.*;
import java.math.BigDecimal;

public record CadastroRegistro(UUID id, String tipo, boolean ativo, Map<String, String> campos, UUID referenciaId) {
    public static final Map<String, Set<String>> CAMPOS = Map.of(
        "grupos", Set.of("nome", "sigla"), "documentos", Set.of("nome", "sigla"),
        "acoes", Set.of("nome", "grupo"), "fases", Set.of("nome", "codigo"),
        "etapas", Set.of("nome", "fase", "classificacao"), "relacoes", Set.of("nome"),
        "tarefas", Set.of("nome", "fase", "pontos"), "origens", Set.of("nome"), "etiquetas", Set.of("nome", "cor"));
    public CadastroRegistro {
        if (tipo == null || !CAMPOS.containsKey(tipo) || campos == null || !CAMPOS.get(tipo).containsAll(campos.keySet()))
            throw new IllegalArgumentException("Tipo ou campos do cadastro inválidos.");
        var normalizados = new HashMap<String, String>();
        for (var campo : CAMPOS.get(tipo)) {
            String valor = campos.getOrDefault(campo, "");
            if (valor == null) throw new IllegalArgumentException("Campo inválido: " + campo);
            valor = valor.strip();
            int limite = campo.equals("nome") ? 150 : (campo.equals("grupo") || campo.equals("fase")) ? 36 : 20;
            if (valor.length() > limite || (campo.equals("nome") && valor.isEmpty())) throw new IllegalArgumentException("Verifique o campo " + campo + ".");
            if (campo.equals("sigla") || campo.equals("codigo")) valor = valor.toUpperCase(Locale.ROOT);
            if (campo.equals("cor")) {
                if (valor.isEmpty()) valor = "#e4dbd2";
                if (!valor.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Cor inválida.");
            }
            if (campo.equals("pontos") || campo.equals("classificacao")) {
                try {
                    var numero = new BigDecimal(valor.isEmpty() ? "0" : valor);
                    if (numero.signum() < 0 || numero.compareTo(new BigDecimal("1000000")) > 0 || numero.stripTrailingZeros().scale() > (campo.equals("classificacao") ? 0 : 2)) throw new NumberFormatException();
                    valor = numero.stripTrailingZeros().toPlainString();
                } catch (NumberFormatException erro) { throw new IllegalArgumentException("Pontuação ou classificação inválida."); }
            }
            normalizados.put(campo, valor);
        }
        String referencia = normalizados.get(tipo.equals("acoes") ? "grupo" : "fase");
        if (CAMPOS.get(tipo).contains("grupo") || CAMPOS.get(tipo).contains("fase")) {
            try { referenciaId = UUID.fromString(referencia); }
            catch (IllegalArgumentException erro) { throw new IllegalArgumentException("Selecione um grupo ou uma fase cadastrada."); }
        } else referenciaId = null;
        campos = Map.copyOf(normalizados);
    }
}
