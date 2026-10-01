package com.advocacia_microservice.usuario.infrastructure.persistence;

import com.advocacia_microservice.usuario.domain.Usuario;
import com.advocacia_microservice.usuario.domain.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioRepositoryImpl implements UsuarioRepository {
    private final JpaUsuarioRepository jpa;

    public UsuarioRepositoryImpl(JpaUsuarioRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        return jpa.saveAndFlush(UsuarioEntity.de(usuario)).paraDominio();
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return jpa.findById(id).map(UsuarioEntity::paraDominio);
    }

    @Override
    public boolean existePorEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public boolean existePorEmailEIdDiferente(String email, UUID id) {
        return jpa.existsByEmailAndIdNot(email, id);
    }

    @Override
    public void excluir(Usuario usuario) {
        jpa.deleteById(usuario.id());
        jpa.flush();
    }
}
