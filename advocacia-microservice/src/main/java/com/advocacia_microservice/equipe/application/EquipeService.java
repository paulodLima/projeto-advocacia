package com.advocacia_microservice.equipe.application;

import com.advocacia_microservice.equipe.api.ConfiguracaoMembroRequest;
import com.advocacia_microservice.equipe.domain.*;
import com.advocacia_microservice.equipe.infrastructure.EmailConviteSender;
import com.advocacia_microservice.equipe.infrastructure.persistence.*;
import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.empresa.infrastructure.persistence.JpaEmpresaUsuarioRepository;
import com.advocacia_microservice.shared.exception.*;
import com.advocacia_microservice.usuario.domain.*;
import com.advocacia_microservice.usuario.infrastructure.persistence.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipeService {
    public record Membro(UUID id, String nome, String email, PapelEmpresa papel, ConfiguracaoMembroRequest configuracao) {}
    public record Convite(UUID id, String nome, String email, PerfilEquipe perfil, String status, Instant expiraEm) {}
    public record Equipe(List<Membro> membros, List<Convite> convites) {}
    public record Acesso(UUID empresaId, PapelEmpresa papel, PerfilEquipe perfil, Set<String> modulos, boolean enviaDocumento) {}
    private final EmpresaService empresas;
    private final EmpresaRepository empresaRepository;
    private final JpaEmpresaUsuarioRepository vinculos;
    private final JpaUsuarioRepository usuarios;
    private final UsuarioPerfilRepository perfis;
    private final JpaUsuarioPerfilRepository perfisJpa;
    private final MembroEquipeRepository membros;
    private final ConviteEquipeRepository convites;
    private final EmailConviteSender email;
    public EquipeService(EmpresaService empresas, EmpresaRepository empresaRepository, JpaEmpresaUsuarioRepository vinculos,
            JpaUsuarioRepository usuarios, UsuarioPerfilRepository perfis, MembroEquipeRepository membros,
            ConviteEquipeRepository convites, EmailConviteSender email, JpaUsuarioPerfilRepository perfisJpa) {
        this.empresas = empresas; this.empresaRepository = empresaRepository; this.vinculos = vinculos;
        this.usuarios = usuarios; this.perfis = perfis; this.membros = membros; this.convites = convites; this.email = email;
        this.perfisJpa = perfisJpa;
    }

    @Transactional(readOnly = true)
    public Acesso acesso(UUID id) {
        var vinculo = empresaRepository.vinculo(id).orElse(null);
        if (vinculo == null) return new Acesso(null, null, null, Set.of("inicio", "config"), false);
        if (vinculo.papel() == PapelEmpresa.MASTER) return new Acesso(vinculo.empresaId(), PapelEmpresa.MASTER, PerfilEquipe.ADMINISTRADOR, PermissoesEquipe.TODOS, true);
        var membro = membro(id);
        return new Acesso(vinculo.empresaId(), PapelEmpresa.MEMBRO, membro.perfil,
                PermissoesEquipe.resolver(membro.perfil, membro.restrito, membro.abas), membro.enviaDocumento);
    }

    @Transactional(readOnly = true)
    public boolean ativo(UUID id) { return membros.findById(id).map(m -> m.situacao == SituacaoEquipe.ATIVO).orElse(true); }

    @Transactional(readOnly = true)
    public Equipe listar(UUID solicitante) {
        var master = empresas.exigirMaster(solicitante);
        var ligados = vinculos.findByEmpresaId(master.empresaId()).stream().map(v -> v.paraDominio()).toList();
        var ids = ligados.stream().map(VinculoEmpresa::usuarioId).collect(java.util.stream.Collectors.toSet());
        var contas = usuarios.findAllById(ids).stream().map(UsuarioEntity::paraDominio).collect(java.util.stream.Collectors.toMap(Usuario::id, u -> u));
        var fichas = membros.findByUsuarioIdIn(ids).stream().collect(java.util.stream.Collectors.toMap(m -> m.usuarioId, m -> m));
        var telefones = perfisJpa.buscarTelefones(ids).stream().collect(java.util.stream.Collectors.toMap(JpaUsuarioPerfilRepository.Telefone::getUsuarioId, JpaUsuarioPerfilRepository.Telefone::getTelefone));
        var lista = ligados.stream().map(v -> {
                    var ficha = fichas.get(v.usuarioId());
                    if (ficha == null) { ficha = new MembroEquipeEntity(); ficha.usuarioId = v.usuarioId(); }
                    return resposta(v, contas.get(v.usuarioId()), ficha, telefones.getOrDefault(v.usuarioId(), ""));
                })
                .sorted(Comparator.comparing(Membro::nome, String.CASE_INSENSITIVE_ORDER)).toList();
        var pendentes = convites.findByEmpresaIdAndStatusOrderByNome(master.empresaId(), "PENDENTE").stream().map(this::resposta).toList();
        return new Equipe(lista, pendentes);
    }

    @Transactional
    public Membro atualizar(UUID solicitante, UUID id, ConfiguracaoMembroRequest request) {
        empresas.autorizarUsuario(solicitante, id, true);
        PermissoesEquipe.resolver(request.perfil(), request.restrito(), request.abas());
        var usuario = usuarios.buscarParaAtualizar(id).orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado."));
        var membro = membro(id);
        membro.nomeExibicao = request.nomeExibicao().strip(); membro.perfil = request.perfil(); membro.situacao = request.situacao();
        membro.admissao = request.admissao(); membro.nascimento = request.nascimento(); membro.restrito = request.restrito();
        membro.enviaDocumento = request.enviaDocumento(); membro.abas = new HashSet<>(request.restrito() ? request.abas() : Set.of());
        membros.saveAndFlush(membro);
        var atual = usuario.paraDominio();
        usuarios.saveAndFlush(UsuarioEntity.de(atual.atualizar(atual.nome(), atual.email(),
                request.situacao() == SituacaoEquipe.ATIVO ? StatusUsuario.ATIVO : StatusUsuario.INATIVO)));
        var perfil = perfis.buscarPorUsuario(id).orElseGet(() -> UsuarioPerfil.vazio(id));
        perfis.salvar(new UsuarioPerfil(id, request.telefone().strip(), perfil.emailPessoal(), perfil.endereco(), perfil.foto()));
        return resposta(empresaRepository.vinculo(id).orElseThrow());
    }

    @Transactional
    public Convite convidar(UUID solicitante, String nome, String endereco, ConfiguracaoMembroRequest request) {
        var master = empresas.exigirMaster(solicitante);
        PermissoesEquipe.resolver(request.perfil(), request.restrito(), request.abas());
        if (request.situacao() != SituacaoEquipe.ATIVO || !request.telefone().isBlank() || request.admissao() != null || request.nascimento() != null) {
            throw new IllegalArgumentException("Preencha a ficha do membro após o primeiro acesso. O convite deve estar ativo.");
        }
        var destinatario = endereco.strip().toLowerCase(Locale.ROOT);
        var usuario = usuarios.findByEmail(destinatario).orElse(null);
        if (usuario != null && (usuario.paraDominio().status() != StatusUsuario.ATIVO || empresaRepository.vinculo(usuario.paraDominio().id()).isPresent())) {
            throw new ConflitoException("Esse e-mail já tem vínculo ou está desativado. Não é possível convidá-lo.");
        }
        var convite = convites.buscarPorEmailParaAtualizar(destinatario).orElseGet(ConviteEquipeEntity::new);
        if ("PENDENTE".equals(convite.status) && convite.expiraEm.isAfter(Instant.now())) throw new ConflitoException("Já existe um convite pendente para esse e-mail.");
        if (convite.id == null) convite.id = UUID.randomUUID();
        convite.empresaId = master.empresaId(); convite.email = destinatario; convite.nome = nome.strip();
        convite.nomeExibicao = request.nomeExibicao().strip(); convite.perfil = request.perfil(); convite.restrito = request.restrito();
        convite.enviaDocumento = request.enviaDocumento(); convite.abas = new HashSet<>(request.restrito() ? request.abas() : Set.of());
        convite.status = "PENDENTE"; convite.expiraEm = Instant.now().plus(Duration.ofDays(7));
        convites.saveAndFlush(convite);
        enviar(convite);
        return resposta(convite);
    }

    @Transactional
    public void cancelar(UUID solicitante, UUID id) {
        var convite = conviteDaEmpresa(solicitante, id);
        convite.status = "CANCELADO"; convites.saveAndFlush(convite);
    }
    @Transactional
    public Convite reenviar(UUID solicitante, UUID id) {
        var convite = conviteDaEmpresa(solicitante, id);
        convite.expiraEm = Instant.now().plus(Duration.ofDays(7));
        convites.saveAndFlush(convite); enviar(convite); return resposta(convite);
    }
    private ConviteEquipeEntity conviteDaEmpresa(UUID solicitante, UUID id) {
        var master = empresas.exigirMaster(solicitante);
        var existente = convites.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Convite não encontrado."));
        var convite = convites.buscarPorEmailParaAtualizar(existente.email).orElseThrow();
        if (!convite.empresaId.equals(master.empresaId()) || !"PENDENTE".equals(convite.status)) throw new AccessDeniedException("Convite indisponível para sua empresa.");
        return convite;
    }

    /** Chamado somente após a validação do e-mail por código ou Google. */
    @Transactional
    public void aceitarConvite(Usuario usuario) {
        if (usuario.status() != StatusUsuario.ATIVO) return;
        var atual = usuarios.buscarParaAtualizar(usuario.id()).orElseThrow().paraDominio();
        if (atual.status() != StatusUsuario.ATIVO || !atual.email().equals(usuario.email())) return;
        if (empresaRepository.vinculo(usuario.id()).isPresent()) return;
        var convite = convites.buscarPorEmailParaAtualizar(usuario.email()).orElse(null);
        if (convite == null || !"PENDENTE".equals(convite.status) || !convite.expiraEm.isAfter(Instant.now())) return;
        empresaRepository.vincular(new VinculoEmpresa(usuario.id(), convite.empresaId, PapelEmpresa.MEMBRO));
        var membro = membro(usuario.id());
        membro.nomeExibicao = convite.nomeExibicao; membro.perfil = convite.perfil; membro.restrito = convite.restrito;
        membro.enviaDocumento = convite.enviaDocumento; membro.abas = new HashSet<>(convite.abas);
        membros.saveAndFlush(membro);
        convite.status = "ACEITO"; convites.saveAndFlush(convite);
    }

    private MembroEquipeEntity membro(UUID id) {
        return membros.findById(id).orElseGet(() -> { var membro = new MembroEquipeEntity(); membro.usuarioId = id; return membro; });
    }
    private Membro resposta(VinculoEmpresa vinculo) {
        var usuario = usuarios.findById(vinculo.usuarioId()).orElseThrow().paraDominio();
        var membro = membro(usuario.id());
        var telefone = perfis.buscarPorUsuario(usuario.id()).map(UsuarioPerfil::telefone).orElse("");
        return resposta(vinculo, usuario, membro, telefone);
    }
    private Membro resposta(VinculoEmpresa vinculo, Usuario usuario, MembroEquipeEntity membro, String telefone) {
        var perfil = vinculo.papel() == PapelEmpresa.MASTER ? PerfilEquipe.ADMINISTRADOR : membro.perfil;
        var situacao = usuario.status() == StatusUsuario.INATIVO && membro.situacao == SituacaoEquipe.ATIVO ? SituacaoEquipe.INATIVO : membro.situacao;
        return new Membro(usuario.id(), usuario.nome(), usuario.email(), vinculo.papel(),
                new ConfiguracaoMembroRequest(membro.nomeExibicao, telefone, membro.admissao, membro.nascimento,
                        perfil, situacao, membro.restrito, Set.copyOf(membro.abas), membro.enviaDocumento));
    }
    private Convite resposta(ConviteEquipeEntity convite) {
        var status = "PENDENTE".equals(convite.status) && !convite.expiraEm.isAfter(Instant.now()) ? "EXPIRADO" : convite.status;
        return new Convite(convite.id, convite.nome, convite.email, convite.perfil, status, convite.expiraEm);
    }
    private void enviar(ConviteEquipeEntity convite) {
        var empresa = empresaRepository.buscar(convite.empresaId).orElseThrow();
        var nome = empresa.dados().get("nome_fantasia");
        email.enviar(convite.email, convite.nome, nome.isBlank() ? empresa.dados().get("razao_social") : nome);
    }
}
