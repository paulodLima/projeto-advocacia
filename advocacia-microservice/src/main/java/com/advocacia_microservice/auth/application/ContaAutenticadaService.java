package com.advocacia_microservice.auth.application;
import com.advocacia_microservice.usuario.domain.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly = true)
public class ContaAutenticadaService {
    private final UsuarioRepository usuarios;
    public ContaAutenticadaService(UsuarioRepository usuarios) { this.usuarios = usuarios; }
    public Optional<Usuario> buscarPorEmail(String email) { return usuarios.buscarPorEmail(email); }
    public Optional<Usuario> buscarAtivo(UUID id) {
        return usuarios.buscarPorId(id).filter(u -> u.status() == StatusUsuario.ATIVO);
    }
}
