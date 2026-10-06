package com.advocacia_microservice.plataforma.application;

import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.StatusUsuario;
import com.advocacia_microservice.usuario.domain.UsuarioRepository;
import com.advocacia_microservice.plataforma.domain.repository.*;
import com.advocacia_microservice.plataforma.application.command.DadosAdministradorInicial;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProvisionarAdministradorService {
    private final AdministracaoRepository administradores;
    private final UsuarioRepository usuarios;
    private final CriarUsuarioUseCase criar;
    private final ProvisionamentoRepository provisionamento;
    private final AuditoriaRepository auditoria;
    private final Validator validator;

    public ProvisionarAdministradorService(AdministracaoRepository administradores,
            UsuarioRepository usuarios, CriarUsuarioUseCase criar, ProvisionamentoRepository provisionamento, AuditoriaRepository auditoria, Validator validator) {
        this.administradores = administradores;
        this.usuarios = usuarios;
        this.criar = criar;
        this.provisionamento = provisionamento;
        this.auditoria = auditoria;
        this.validator = validator;
    }

    @Transactional
    public void executar(String email, String nome) {
        if (email == null || email.isBlank()) return;
        boolean concluido = provisionamento.bloquearEConsultarConclusao();
        // A configuração de inicialização nunca reativa ou substitui um administrador provisionado.
        if (concluido) return;
        var dados = new DadosAdministradorInicial(email.strip().toLowerCase(java.util.Locale.ROOT), nome == null ? "" : nome.strip());
        if (!validator.validate(dados).isEmpty()) {
            throw new IllegalArgumentException("Configure email e nome válidos para o administrador inicial.");
        }
        if (administradores.contar() > 0) {
            provisionamento.concluir();
            return;
        }
        var usuario = usuarios.buscarPorEmail(dados.email())
                .orElseGet(() -> criar.executar(dados.nome(), dados.email()));
        if (usuario.status() != StatusUsuario.ATIVO) {
            throw new IllegalArgumentException("A conta do administrador inicial precisa estar ativa.");
        }
        administradores.conceder(usuario.id());
        auditoria.registrar(null, "ADMINISTRADOR_INICIAL_PROVISIONADO", usuario.id());
        provisionamento.concluir();
    }
}
