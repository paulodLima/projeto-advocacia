package com.advocacia_microservice.escritorio.infrastructure.persistence.repository;
import com.advocacia_microservice.escritorio.domain.*;
import com.advocacia_microservice.escritorio.domain.repository.EmpresaRepository;
import com.advocacia_microservice.escritorio.infrastructure.persistence.entity.EscritorioEntity;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
@Repository
public class EmpresaRepositoryImpl implements EmpresaRepository {
    private final EscritorioRepository jpa;
    public EmpresaRepositoryImpl(EscritorioRepository jpa) { this.jpa = jpa; }
    public Empresa salvar(Empresa e) { return jpa.saveAndFlush(EscritorioEntity.de(e)).paraDominio(); }
    public Optional<Empresa> buscarPorId(UUID id) { return jpa.findById(id).map(EscritorioEntity::paraDominio); }
    public List<Empresa> buscarPorIds(List<UUID> ids) { return jpa.findAllById(ids).stream().map(EscritorioEntity::paraDominio).toList(); }
    public PaginaEmpresas listar(int pagina, int tamanho) {
        var p = jpa.findAll(PageRequest.of(pagina, tamanho, Sort.by("nome", "id")));
        return new PaginaEmpresas(p.getContent().stream().map(EscritorioEntity::paraDominio).toList(), p.getTotalElements(), pagina, p.getTotalPages());
    }
}
