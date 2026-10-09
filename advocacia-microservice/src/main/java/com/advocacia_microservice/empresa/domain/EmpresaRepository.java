package com.advocacia_microservice.empresa.domain;

import java.util.Optional;
import java.util.UUID;

public interface EmpresaRepository {
    Optional<Empresa> buscar(UUID id);
    Empresa salvar(Empresa empresa);
    boolean existeOutroCnpj(String cnpj, UUID id);
    Optional<VinculoEmpresa> vinculo(UUID usuarioId);
    void vincular(VinculoEmpresa vinculo);
}
