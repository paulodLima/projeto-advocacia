package com.advocacia_microservice.usuario.domain;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioPerfilRepository {
    Optional<UsuarioPerfil> buscarPorUsuario(UUID id);
    UsuarioPerfil salvar(UsuarioPerfil perfil);
}
