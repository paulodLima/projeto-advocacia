package com.advocacia_microservice.usuario.application;

import com.advocacia_microservice.shared.exception.ConflitoException;
import com.advocacia_microservice.usuario.domain.StatusUsuario;
import com.advocacia_microservice.usuario.domain.Usuario;
import com.advocacia_microservice.usuario.domain.UsuarioRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtualizarUsuarioUseCase {
    private final UsuarioRepository repository;
    private final BuscarUsuarioUseCase buscar;

    public AtualizarUsuarioUseCase(UsuarioRepository repository, BuscarUsuarioUseCase buscar) {
        this.repository = repository;
        this.buscar = buscar;
    }

    @Transactional
    public Usuario executar(UUID id, String nome, String email, StatusUsuario status) {
        Usuario usuario = buscar.executar(id).atualizar(nome, email, status);
        if (repository.existePorEmailEIdDiferente(usuario.email(), id)) {
            throw new ConflitoException("Já existe um usuário com este email.");
        }
        return repository.salvar(usuario);
    }
}
