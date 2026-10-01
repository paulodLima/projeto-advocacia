package com.advocacia_microservice.usuario.application;

import com.advocacia_microservice.shared.exception.RecursoNaoEncontradoException;
import com.advocacia_microservice.usuario.domain.Usuario;
import com.advocacia_microservice.usuario.domain.UsuarioRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuscarUsuarioUseCase {
    private final UsuarioRepository repository;

    public BuscarUsuarioUseCase(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Usuario executar(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }
}
