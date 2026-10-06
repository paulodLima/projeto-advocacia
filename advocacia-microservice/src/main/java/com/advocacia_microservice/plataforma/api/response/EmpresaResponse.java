package com.advocacia_microservice.plataforma.api.response;
import com.advocacia_microservice.escritorio.domain.Empresa;
import java.util.UUID;
public record EmpresaResponse(UUID id, String nome, String identificacao, boolean ativo, String razaoSocial,
        String cnpj, Integer quantidadeFuncionarios, String email, String telefone, String endereco,
        String cidade, String uf, String cep) {
    public static EmpresaResponse de(Empresa e) {
        return new EmpresaResponse(e.id(), e.nome(), e.identificacao(), e.ativo(), e.razaoSocial(), e.cnpj(),
                e.quantidadeFuncionarios(), e.email(), e.telefone(), e.endereco(), e.cidade(), e.uf(), e.cep());
    }
}
