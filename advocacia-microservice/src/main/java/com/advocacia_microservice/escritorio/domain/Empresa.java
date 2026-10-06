package com.advocacia_microservice.escritorio.domain;
import java.util.UUID;
public record Empresa(UUID id, String nome, String identificacao, boolean ativo, String razaoSocial,
        String cnpj, Integer quantidadeFuncionarios, String email, String telefone,
        String endereco, String cidade, String uf, String cep) {}
