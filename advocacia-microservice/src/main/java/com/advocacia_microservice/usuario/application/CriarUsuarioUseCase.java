package com.advocacia_microservice.usuario.application;

import com.advocacia_microservice.shared.exception.ConflitoException;
import com.advocacia_microservice.usuario.domain.Usuario;
import com.advocacia_microservice.usuario.domain.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CriarUsuarioUseCase {
    private final UsuarioRepository repository;

    public CriarUsuarioUseCase(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Usuario executar(String nome, String email) {
        Usuario usuario = Usuario.criar(nome, email);
        if (repository.existePorEmail(usuario.email())) {
            throw new ConflitoException("Já existe um usuário com este email.");
        }
        return repository.salvar(usuario);
    }
}
