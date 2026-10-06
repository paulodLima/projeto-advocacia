package com.advocacia_microservice.plataforma.application;
import com.advocacia_microservice.plataforma.domain.repository.AdministracaoRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AcessoPlataformaService {
    private final AdministracaoRepository administradores;
    public AcessoPlataformaService(AdministracaoRepository administradores) { this.administradores = administradores; }
    @Transactional(readOnly = true)
    public boolean podeAdministrar(String identidade) {
        try { return administradores.estaAtivo(UUID.fromString(identidade)); }
        catch (IllegalArgumentException | NullPointerException exception) { return false; }
    }
}
