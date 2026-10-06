package com.advocacia_microservice.plataforma.domain;

import java.util.Locale;

public final class Cnpj {
    private Cnpj() {}
    public static String normalizar(String valor) {
        String cnpj = valor == null ? "" : valor.strip().toUpperCase(Locale.ROOT).replaceAll("[./-]", "");
        if (!cnpj.matches("[A-Z0-9]{12}[0-9]{2}") || cnpj.matches("([0-9])\\1{13}"))
            throw new IllegalArgumentException("Informe um CNPJ válido.");
        String base = cnpj.substring(0, 12);
        int primeiro = digito(base);
        if (!cnpj.endsWith("" + primeiro + digito(base + primeiro)))
            throw new IllegalArgumentException("Informe um CNPJ válido.");
        return cnpj;
    }
    private static int digito(String base) {
        int soma = 0, peso = 2;
        for (int i = base.length() - 1; i >= 0; i--) {
            soma += (base.charAt(i) - 48) * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
