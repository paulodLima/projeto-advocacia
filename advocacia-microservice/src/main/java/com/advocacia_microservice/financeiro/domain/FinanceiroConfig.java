package com.advocacia_microservice.financeiro.domain;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

public final class FinanceiroConfig {
    private FinanceiroConfig() {}
    public static final List<String> GRUPOS = List.of("0 - Deduções da receita bruta", "1 - Receitas", "2 - Folha de pagamento", "3 - Encargos sociais", "4 - Tributos", "5 - Despesas fixas", "6 - Despesas variáveis", "7 - Despesas bancárias", "8 - Marketing e comercial", "9 - Distribuição de lucros e participação");
    public static String nome(String nome) {
        if (nome == null || nome.isBlank() || nome.strip().length() > 150) throw new IllegalArgumentException("Informe um nome de até 150 caracteres.");
        return nome.strip();
    }
    private static BigDecimal numero(BigDecimal valor, String campo, String min, String max) {
        if (valor == null || valor.compareTo(new BigDecimal(min)) < 0 || valor.compareTo(new BigDecimal(max)) > 0 || valor.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException(campo + " deve estar entre " + min + " e " + max + ", com até duas casas decimais.");
        return valor.setScale(2);
    }
    public record Conta(UUID id, String nome, String banco, String tipo, BigDecimal saldo, boolean padrao, boolean ativo) {
        public Conta {
            nome = FinanceiroConfig.nome(nome);
            if (banco == null || banco.strip().length() > 150) throw new IllegalArgumentException("Banco deve ter até 150 caracteres.");
            banco = banco.strip();
            if (tipo == null || !Set.of("conta", "cartao", "caixa", "investimento").contains(tipo)) throw new IllegalArgumentException("Tipo de conta inválido.");
            saldo = numero(saldo, "Saldo inicial", "-999999999999.99", "999999999999.99");
            if (padrao && !ativo) throw new IllegalArgumentException("A conta padrão precisa estar ativa.");
        }
    }
    public record Categoria(UUID id, String nome, String grupo, String direcao, boolean ativo) {
        public Categoria {
            nome = FinanceiroConfig.nome(nome);
            if (grupo == null || !GRUPOS.contains(grupo) || direcao == null || !Set.of("entrada", "saida", "ambas").contains(direcao)) throw new IllegalArgumentException("Selecione grupo e tipo válidos para a categoria.");
        }
    }
    public record Centro(UUID id, String nome, boolean ativo) { public Centro { nome = FinanceiroConfig.nome(nome); } }
    public record Custo(UUID id, String nome, BigDecimal valor) { public Custo { nome = FinanceiroConfig.nome(nome); valor = numero(valor, "Custo mensal", "0.01", "999999999999.99"); } }
    public record Urh(BigDecimal urh, String competencia) {
        public Urh {
            urh = numero(urh, "URH", "0.01", "999999999999.99");
            if (competencia == null || !competencia.matches("[0-9]{4}-(0[1-9]|1[0-2])") || competencia.startsWith("0000")) throw new IllegalArgumentException("Informe a competência no formato AAAA-MM.");
            YearMonth.parse(competencia);
        }
    }
    public record Capacidade(BigDecimal horas, String modoHoras, Integer advogados, BigDecimal horasSemanais, BigDecimal percentualProdutivo, BigDecimal semanasPorMes, BigDecimal margemLucro, BigDecimal fatorPosicionamento) {
        public Capacidade {
            horas = numero(horas, "Horas produtivas", "1", "1000000");
            if (modoHoras == null || !Set.of("direto", "estrutura").contains(modoHoras) || advogados == null || advogados < 1 || advogados > 10000) throw new IllegalArgumentException("Informe o modo de cálculo e de 1 a 10000 advogados.");
            horasSemanais = numero(horasSemanais, "Horas semanais", "1", "168");
            percentualProdutivo = numero(percentualProdutivo, "Percentual produtivo", "1", "100");
            semanasPorMes = numero(semanasPorMes, "Semanas por mês", "1", "5");
            margemLucro = numero(margemLucro, "Margem de lucro", "0", "100");
            fatorPosicionamento = numero(fatorPosicionamento, "Fator de posicionamento", "1.5", "10");
        }
        public static Capacidade padrao() { return new Capacidade(new BigDecimal("160"), "estrutura", 1, new BigDecimal("40"), new BigDecimal("60"), new BigDecimal("4.2"), new BigDecimal("30"), new BigDecimal("3")); }
    }
}
