package com.advocacia_microservice.escritorio.infrastructure.persistence.repository;
import com.advocacia_microservice.escritorio.domain.VinculoEscritorio;
import com.advocacia_microservice.escritorio.domain.repository.VinculoRepository;
import com.advocacia_microservice.escritorio.infrastructure.persistence.entity.MembroEntity;
import java.util.*;
import org.springframework.stereotype.Repository;
@Repository
public class VinculoRepositoryImpl implements VinculoRepository {
    private final MembroRepository jpa;
    public VinculoRepositoryImpl(MembroRepository jpa) { this.jpa = jpa; }
    public void salvar(VinculoEscritorio v) {
        var e = new MembroEntity(); e.id = v.id(); e.escritorioId = v.escritorioId();
        e.usuarioId = v.usuarioId(); e.perfil = v.perfil(); e.ativo = v.ativo(); jpa.save(e);
    }
    private VinculoEscritorio dominio(MembroEntity e) { return new VinculoEscritorio(e.id, e.escritorioId, e.usuarioId, e.perfil, e.ativo); }
    public List<VinculoEscritorio> listarAtivos(UUID id) { return jpa.findByUsuarioIdAndAtivoTrue(id).stream().map(this::dominio).toList(); }
    public Optional<VinculoEscritorio> buscarAtivo(UUID empresa, UUID usuario) { return jpa.findByEscritorioIdAndUsuarioIdAndAtivoTrue(empresa, usuario).map(this::dominio); }
}
