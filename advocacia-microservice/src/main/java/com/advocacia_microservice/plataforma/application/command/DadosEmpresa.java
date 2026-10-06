package com.advocacia_microservice.plataforma.application.command;
public record DadosEmpresa(String nome, String identificacao, Boolean ativo, String razaoSocial, String cnpj, java.math.BigDecimal quantidadeFuncionarios, String email, String telefone, String endereco, String cidade, String uf, String cep) {}
