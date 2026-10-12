package com.advocacia_microservice.empresa.infrastructure.persistence.repository;
import com.advocacia_microservice.empresa.infrastructure.persistence.entity.EmpresaEntity;
import com.advocacia_microservice.empresa.infrastructure.persistence.entity.EmpresaUsuarioEntity;
import com.advocacia_microservice.empresa.infrastructure.persistence.repository.JpaEmpresaRepository;
import com.advocacia_microservice.empresa.infrastructure.persistence.repository.JpaEmpresaUsuarioRepository;

import com.advocacia_microservice.empresa.domain.*;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository("empresaConfiguracaoRepositoryImpl")
@Transactional(readOnly = true)
public class EmpresaRepositoryImpl implements EmpresaRepository {
    private final JpaEmpresaRepository empresas;
    private final JpaEmpresaUsuarioRepository vinculos;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final com.advocacia_microservice.escritorio.infrastructure.persistence.repository.EscritorioRepository escritorios;
    private final com.advocacia_microservice.equipe.infrastructure.persistence.repository.ConviteEquipeRepository convitesEquipe;
    public EmpresaRepositoryImpl(JpaEmpresaRepository empresas, JpaEmpresaUsuarioRepository vinculos,
            com.advocacia_microservice.escritorio.infrastructure.persistence.repository.EscritorioRepository escritorios,
            com.advocacia_microservice.equipe.infrastructure.persistence.repository.ConviteEquipeRepository convitesEquipe,
            org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.empresas = empresas; this.vinculos = vinculos; this.escritorios = escritorios; this.convitesEquipe = convitesEquipe;
        this.jdbc = jdbc;
    }
    public Optional<Empresa> buscar(UUID id) { return empresas.findById(id).map(EmpresaEntity::paraDominio); }
    @Transactional
    public Empresa salvar(Empresa empresa) {
        boolean nova = !escritorios.existsById(empresa.id());
        var legado = escritorios.findById(empresa.id()).orElseGet(com.advocacia_microservice.escritorio.infrastructure.persistence.entity.EscritorioEntity::new);
        legado.id = empresa.id(); legado.identificacao = legado.identificacao == null ? "empresa-" + empresa.id() : legado.identificacao;
        var dados = empresa.dados();
        legado.nome = dados.get("nome_fantasia").isBlank() ? dados.get("razao_social") : dados.get("nome_fantasia");
        legado.razaoSocial = dados.get("razao_social");
        legado.cnpj = dados.get("cnpj").length() == 14 ? dados.get("cnpj") : null;
        legado.endereco = dados.get("endereco").substring(0, Math.min(300, dados.get("endereco").length()));
        legado.cidade = dados.get("cidade"); legado.uf = dados.get("uf"); legado.email = dados.get("email"); legado.telefone = dados.get("telefone");
        legado.cep = dados.get("cep").replaceAll("\\D", "");
        if (legado.cep.length() > 8) legado.cep = legado.cep.substring(0,8);
        escritorios.saveAndFlush(legado);
        if (nova) jdbc.update("INSERT INTO empresa_modulo(empresa_id,modulo_codigo,liberado) SELECT ?,codigo,TRUE FROM modulo_plataforma",empresa.id());
        return empresas.saveAndFlush(EmpresaEntity.de(empresa)).paraDominio();
    }
    public boolean existeOutroCnpj(String cnpj, UUID id) { return empresas.existsByCnpjAndIdNot(cnpj, id); }
    public Optional<VinculoEmpresa> vinculo(UUID id) { return vinculos.findById(id).map(EmpresaUsuarioEntity::paraDominio); }
    @Transactional
    public void vincular(VinculoEmpresa vinculo) { vinculos.saveAndFlush(EmpresaUsuarioEntity.de(vinculo)); }
    public boolean estaAtiva(UUID empresaId) { return escritorios.findById(empresaId).map(e -> e.ativo).orElse(false); }
    public boolean temConviteEquipePendente(String email) {
        return convitesEquipe.findByEmail(email).filter(c -> "PENDENTE".equals(c.status) && c.expiraEm.isAfter(java.time.Instant.now()))
                .filter(c -> estaAtiva(c.empresaId)).isPresent();
    }
}
