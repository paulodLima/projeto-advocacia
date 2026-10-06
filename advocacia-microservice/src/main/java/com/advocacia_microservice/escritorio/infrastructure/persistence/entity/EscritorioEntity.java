package com.advocacia_microservice.escritorio.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "escritorio")
public class EscritorioEntity {
    @Id public UUID id;
    @Column(nullable = false, length = 150) public String nome;
    @Column(nullable = false, length = 80, unique = true) public String identificacao;
    @Column(nullable = false) public boolean ativo = true;
    @Column(length = 150) public String razaoSocial;
    @Column(length = 14, unique = true) public String cnpj;
    public Integer quantidadeFuncionarios;
    @Column(length = 254) public String email;
    @Column(length = 30) public String telefone;
    @Column(length = 300) public String endereco;
    @Column(length = 100) public String cidade;
    @Column(length = 2) public String uf;
    @Column(length = 8) public String cep;
    public com.advocacia_microservice.escritorio.domain.Empresa paraDominio() {
        return new com.advocacia_microservice.escritorio.domain.Empresa(id, nome, identificacao, ativo, razaoSocial,
                cnpj, quantidadeFuncionarios, email, telefone, endereco, cidade, uf, cep);
    }
    public static EscritorioEntity de(com.advocacia_microservice.escritorio.domain.Empresa empresa) {
        var e = new EscritorioEntity(); e.id = empresa.id(); e.nome = empresa.nome();
        e.identificacao = empresa.identificacao(); e.ativo = empresa.ativo(); e.razaoSocial = empresa.razaoSocial();
        e.cnpj = empresa.cnpj(); e.quantidadeFuncionarios = empresa.quantidadeFuncionarios();
        e.email = empresa.email(); e.telefone = empresa.telefone(); e.endereco = empresa.endereco();
        e.cidade = empresa.cidade(); e.uf = empresa.uf(); e.cep = empresa.cep(); return e;
    }
}
