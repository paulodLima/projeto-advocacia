package com.advocacia_microservice.usuario.infrastructure.persistence;

import com.advocacia_microservice.usuario.domain.*;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioPerfilRepositoryImpl implements UsuarioPerfilRepository {
    private final JpaUsuarioPerfilRepository jpa;
    public UsuarioPerfilRepositoryImpl(JpaUsuarioPerfilRepository jpa) { this.jpa = jpa; }
    public Optional<UsuarioPerfil> buscarPorUsuario(UUID id) {
        return jpa.findById(id).map(UsuarioPerfilEntity::paraDominio);
    }
    public UsuarioPerfil salvar(UsuarioPerfil perfil) {
        return jpa.saveAndFlush(UsuarioPerfilEntity.de(perfil)).paraDominio();
    }
}
