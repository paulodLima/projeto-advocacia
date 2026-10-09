package com.advocacia_microservice.usuario.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUsuarioPerfilRepository extends JpaRepository<UsuarioPerfilEntity, UUID> {
    interface Telefone { UUID getUsuarioId(); String getTelefone(); }
    @org.springframework.data.jpa.repository.Query("select p.usuarioId as usuarioId, p.telefone as telefone from UsuarioPerfilEntity p where p.usuarioId in :ids")
    java.util.List<Telefone> buscarTelefones(@org.springframework.data.repository.query.Param("ids") java.util.Set<UUID> ids);
}
