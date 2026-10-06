package com.advocacia_microservice.plataforma.api.request;
import com.advocacia_microservice.plataforma.application.command.DadosEmpresa;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record SalvarEmpresaRequest(
    @NotBlank @Size(max = 150) String nome,
    @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9-]{1,79}", message = "Use 2 a 80 letras minúsculas, números ou hífens") String identificacao,
    @NotNull Boolean ativo, @NotBlank @Size(max = 150) String razaoSocial,
    @NotBlank @Size(max = 18) String cnpj,
    @NotNull @Min(0) @Digits(integer = 9, fraction = 0) BigDecimal quantidadeFuncionarios,
    @Email @Size(max = 254) String email, @Size(max = 30) String telefone,
    @Size(max = 300) String endereco, @Size(max = 100) String cidade,
    @Pattern(regexp = "^$|AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO") String uf,
    @Pattern(regexp = "^$|[0-9]{5}-?[0-9]{3}") String cep) {
    public DadosEmpresa paraComando() {
        return new DadosEmpresa(nome, identificacao, ativo, razaoSocial, cnpj, quantidadeFuncionarios,
                email, telefone, endereco, cidade, uf, cep);
    }
}
