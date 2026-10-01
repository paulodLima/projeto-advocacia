package com.advocacia_microservice.usuario.application;

import com.advocacia_microservice.usuario.domain.UsuarioRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExcluirUsuarioUseCase {
    private final UsuarioRepository repository;
    private final BuscarUsuarioUseCase buscar;

    public ExcluirUsuarioUseCase(UsuarioRepository repository, BuscarUsuarioUseCase buscar) {
        this.repository = repository;
        this.buscar = buscar;
    }

    @Transactional
    public void executar(UUID id) {
        repository.excluir(buscar.executar(id));
    }
}
