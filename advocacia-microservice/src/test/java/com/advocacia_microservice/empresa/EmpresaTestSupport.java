package com.advocacia_microservice.empresa;

import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.plataforma.domain.ConviteInicial;
import com.advocacia_microservice.plataforma.domain.repository.ConviteInicialRepository;
import com.advocacia_microservice.usuario.domain.UsuarioRepository;
import java.time.Instant;
import java.util.*;

/** Fixtures de módulos partem de um responsável com convite já confirmado. */
public final class EmpresaTestSupport {
    private EmpresaTestSupport() {}
    public static EmpresaService.MinhaEmpresa criar(EmpresaService empresas, ConviteInicialRepository convites,
            UsuarioRepository usuarios, UUID usuarioId, Map<String, String> dados) {
        var usuario = usuarios.buscarPorId(usuarioId).orElseThrow();
        var existente = convites.buscarPorEmail(usuario.email()).orElse(null);
        if (existente == null) convites.salvar(new ConviteInicial(UUID.randomUUID(), usuario.nome(), usuario.email(), "ACEITO",
                Instant.now().plusSeconds(86400), usuario.id(), null));
        return empresas.criar(usuarioId, dados);
    }
}
