package com.advocacia_microservice.empresa.domain;

import java.util.Arrays;

public enum CampoEmpresa {
    RAZAO_SOCIAL(150, true), NOME_FANTASIA(150), CNPJ(32, true), OAB_SOCIEDADE(50),
    INSCRICAO_MUNICIPAL(50), CNAE(20), CONSTITUIDA_EM(10), TITULAR_NOME(150),
    TITULAR_CPF(20), OAB(50), ENDERECO(500, true), BAIRRO(100), CIDADE(100, true),
    UF(2), CEP(12), TELEFONE(30), WHATSAPP(30), EMAIL(254), SITE(500), REGIME(100),
    CODIGO_SERVICO(50), ISS_ALIQUOTA(20), OBSERVACAO_FISCAL(1000), BANCO_DADOS(1000);

    public final int limite;
    public final boolean obrigatorio;
    CampoEmpresa(int limite) { this(limite, false); }
    CampoEmpresa(int limite, boolean obrigatorio) { this.limite = limite; this.obrigatorio = obrigatorio; }
    public String chave() { return name().toLowerCase(java.util.Locale.ROOT); }
    public static boolean conhecido(String chave) {
        return Arrays.stream(values()).anyMatch(campo -> campo.chave().equals(chave));
    }
}
