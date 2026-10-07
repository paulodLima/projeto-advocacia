package com.advocacia_microservice.empresa.infrastructure.persistence;

import com.advocacia_microservice.empresa.domain.*;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class EmpresaRepositoryImpl implements EmpresaRepository {
    private final JpaEmpresaRepository empresas;
    private final JpaEmpresaUsuarioRepository vinculos;
    public EmpresaRepositoryImpl(JpaEmpresaRepository empresas, JpaEmpresaUsuarioRepository vinculos) {
        this.empresas = empresas; this.vinculos = vinculos;
    }
    public Optional<Empresa> buscar(UUID id) { return empresas.findById(id).map(EmpresaEntity::paraDominio); }
    @Transactional
    public Empresa salvar(Empresa empresa) { return empresas.saveAndFlush(EmpresaEntity.de(empresa)).paraDominio(); }
    public boolean existeOutroCnpj(String cnpj, UUID id) { return empresas.existsByCnpjAndIdNot(cnpj, id); }
    public Optional<VinculoEmpresa> vinculo(UUID id) { return vinculos.findById(id).map(EmpresaUsuarioEntity::paraDominio); }
    @Transactional
    public void vincular(VinculoEmpresa vinculo) { vinculos.saveAndFlush(EmpresaUsuarioEntity.de(vinculo)); }
}
