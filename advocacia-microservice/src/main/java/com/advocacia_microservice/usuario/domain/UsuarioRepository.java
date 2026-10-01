package com.advocacia_microservice.usuario.domain;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository {
    Usuario salvar(Usuario usuario);
    Optional<Usuario> buscarPorId(UUID id);
    boolean existePorEmail(String email);
    boolean existePorEmailEIdDiferente(String email, UUID id);
    void excluir(Usuario usuario);
}
