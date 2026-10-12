package com.advocacia_microservice.plataforma.application;

import com.advocacia_microservice.plataforma.domain.ConviteInicial;
import com.advocacia_microservice.plataforma.domain.repository.*;
import com.advocacia_microservice.empresa.domain.EmpresaRepository;
import com.advocacia_microservice.usuario.domain.*;
import com.advocacia_microservice.shared.exception.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConvitesIniciaisService {
    private final ConviteInicialRepository convites;
    private final AdministracaoRepository administradores;
    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;
    private final AuditoriaRepository auditoria;
    private final NotificacaoConviteInicial notificacao;
    public ConvitesIniciaisService(ConviteInicialRepository convites, AdministracaoRepository administradores,
            UsuarioRepository usuarios, EmpresaRepository empresas, AuditoriaRepository auditoria, NotificacaoConviteInicial notificacao) {
        this.convites = convites; this.administradores = administradores; this.usuarios = usuarios;
        this.empresas = empresas; this.auditoria = auditoria; this.notificacao = notificacao;
    }
    private void exigirAdministrador(UUID autor) {
        if (!administradores.estaAtivo(autor)) throw new AccessDeniedException("Somente o administrador da plataforma pode enviar o primeiro convite.");
    }
    public List<ConviteInicial> listar(UUID autor) { exigirAdministrador(autor); return convites.listar(); }
    public boolean permiteCadastro(String email) {
        return convites.buscarPorEmail(email).filter(c -> c.permiteEntrada(Instant.now())).isPresent();
    }
    public boolean permiteCriarEscritorio(Usuario usuario) {
        return convites.buscarPorEmail(usuario.email()).filter(c -> c.status().equals("ACEITO")
                && usuario.id().equals(c.usuarioId()) && c.permiteEntrada(Instant.now())).isPresent();
    }
    @Transactional
    public ConviteInicial convidar(UUID autor, String nome, String email) {
        exigirAdministrador(autor);
        var destino = Usuario.criar(nome, email);
        if (empresas.temConviteEquipePendente(destino.email())) throw new ConflitoException("Este email já possui um convite para integrar uma equipe.");
        var conta = usuarios.buscarPorEmail(destino.email()).orElse(null);
        if (conta != null && (conta.status() != StatusUsuario.ATIVO || administradores.estaAtivo(conta.id()) || empresas.vinculo(conta.id()).isPresent())) {
            throw new ConflitoException("Este email já possui um escritório, é administrador da plataforma ou está desativado.");
        }
        var existente = convites.bloquearPorEmail(destino.email()).orElse(null);
        if (existente != null && (existente.permiteEntrada(Instant.now()) || existente.status().equals("CONCLUIDO"))) {
            throw new ConflitoException("Já existe um convite para este email. Reenvie o convite pendente.");
        }
        var convite = new ConviteInicial(existente == null ? UUID.randomUUID() : existente.id(), destino.nome(), destino.email(),
                "PENDENTE", Instant.now().plus(Duration.ofDays(7)), null, null);
        convites.salvar(convite); notificacao.enviar(convite.email(), convite.nome());
        auditoria.registrar(autor, "CONVITE_INICIAL_ENVIADO", convite.id()); return convite;
    }
    @Transactional
    public ConviteInicial reenviar(UUID autor, UUID id) {
        exigirAdministrador(autor); var c = exigirEditavel(id);
        var convite = new ConviteInicial(c.id(), c.nome(), c.email(), c.status(), Instant.now().plus(Duration.ofDays(7)), c.usuarioId(), null);
        convites.salvar(convite); notificacao.enviar(c.email(), c.nome());
        auditoria.registrar(autor, "CONVITE_INICIAL_REENVIADO", id); return convite;
    }
    @Transactional
    public void cancelar(UUID autor, UUID id) {
        exigirAdministrador(autor); var c = exigirEditavel(id);
        convites.salvar(new ConviteInicial(c.id(), c.nome(), c.email(), "CANCELADO", c.expiraEm(), c.usuarioId(), null));
        auditoria.registrar(autor, "CONVITE_INICIAL_CANCELADO", id);
    }
    private ConviteInicial exigirEditavel(UUID id) {
        var encontrado = convites.buscar(id).orElseThrow(() -> new RecursoNaoEncontradoException("Convite não encontrado."));
        var c = convites.bloquearPorEmail(encontrado.email()).orElseThrow();
        if (!Set.of("PENDENTE", "ACEITO").contains(c.status())) throw new ConflitoException("Este convite não pode mais ser alterado.");
        return c;
    }
    @Transactional
    public void aceitar(Usuario usuario) {
        var c = convites.bloquearPorEmail(usuario.email()).orElse(null);
        if (c == null || !c.permiteEntrada(Instant.now()) || empresas.vinculo(usuario.id()).isPresent()) return;
        if (c.usuarioId() != null && !c.usuarioId().equals(usuario.id())) throw new AccessDeniedException("Convite vinculado a outra conta.");
        convites.salvar(c.aceitar(usuario.id()));
    }
    @Transactional
    public void autorizarEConcluir(Usuario usuario, UUID empresaId) {
        var c = convites.bloquearPorEmail(usuario.email()).orElseThrow(() -> new AccessDeniedException("Solicite o primeiro convite ao administrador da plataforma."));
        if (!c.status().equals("ACEITO") || !usuario.id().equals(c.usuarioId()) || !c.permiteEntrada(Instant.now())) {
            throw new AccessDeniedException("Seu convite foi cancelado, expirou ou ainda não foi confirmado.");
        }
        convites.salvar(c.concluir(empresaId));
        auditoria.registrar(usuario.id(), "ESCRITORIO_CADASTRADO_PELO_CONVIDADO", empresaId);
    }
}
