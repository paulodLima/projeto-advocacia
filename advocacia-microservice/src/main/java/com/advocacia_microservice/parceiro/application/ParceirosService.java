package com.advocacia_microservice.parceiro.application;

import com.advocacia_microservice.cliente.domain.Contato;
import com.advocacia_microservice.cliente.infrastructure.persistence.ContatoRepository;
import com.advocacia_microservice.equipe.application.EquipeService;
import com.advocacia_microservice.equipe.domain.PerfilEquipe;
import com.advocacia_microservice.empresa.domain.PapelEmpresa;
import com.advocacia_microservice.parceiro.domain.Parceiro;
import com.advocacia_microservice.parceiro.infrastructure.persistence.ParceiroRepository;
import com.advocacia_microservice.shared.exception.ConflitoException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ParceirosService {
    public record Formulario(Integer versao, Map<String, String> dados, List<Parceiro.Socio> socios, List<String> areasAtuacao) {}
    public record Pagina(List<Parceiro> itens, long total, int pagina, int tamanho) {}
    public record Opcoes(boolean podeEditar, boolean podeVerContatos, List<String> estados, List<String> areas) {}
    public record Cliente(UUID id, String nome, String tipo) {}
    public record Carteira(List<Cliente> itens, long total, int pagina, int tamanho) {}
    private final EquipeService equipe;
    private final ContatoRepository contatos;
    private final ParceiroRepository parceiros;
    private final JdbcTemplate jdbc;

    public ParceirosService(EquipeService equipe, ContatoRepository contatos, ParceiroRepository parceiros, JdbcTemplate jdbc) {
        this.equipe = equipe; this.contatos = contatos; this.parceiros = parceiros; this.jdbc = jdbc;
    }
    private boolean edita(EquipeService.Acesso a) {
        return a.papel() == PapelEmpresa.MASTER || Set.of(PerfilEquipe.ADMINISTRADOR, PerfilEquipe.ADVOGADO, PerfilEquipe.ASSISTENTE).contains(a.perfil());
    }
    private EquipeService.Acesso acesso(UUID usuario, boolean escrita) {
        var a = equipe.acesso(usuario);
        if (a.empresaId() == null || !equipe.ativo(usuario) || !a.modulos().contains("parceiros"))
            throw new AccessDeniedException("Você não tem acesso aos parceiros desta empresa.");
        if (escrita && !edita(a)) throw new AccessDeniedException("Seu perfil permite apenas consultar parceiros.");
        return a;
    }
    public Opcoes opcoes(UUID usuario) {
        var a = acesso(usuario, false);
        return new Opcoes(edita(a), a.modulos().contains("contatos"), parceiros.estados(a.empresaId()), Parceiro.AREAS);
    }
    private void pagina(int pagina, int tamanho) {
        if (pagina < 0 || pagina > 1000000 || tamanho < 1 || tamanho > 100) throw new IllegalArgumentException("Paginação inválida.");
    }
    public Pagina listar(UUID usuario, String busca, String uf, String area, int pagina, int tamanho) {
        var empresa = acesso(usuario, false).empresaId(); pagina(pagina, tamanho);
        if (busca == null || busca.length() > 150 || uf == null || !uf.matches("[A-Z]{2}|^$") || area == null || (!area.isEmpty() && !Parceiro.AREAS.contains(area)))
            throw new IllegalArgumentException("Filtros inválidos.");
        var filtro = parceiros.filtro(empresa, busca.strip(), uf, area);
        var ids = parceiros.ids(filtro, pagina, tamanho);
        return new Pagina(parceiros.carregar(empresa, contatos.carregarPagina(empresa, ids)), parceiros.total(filtro), pagina, tamanho);
    }
    public Parceiro buscar(UUID usuario, UUID id) { return parceiros.buscar(acesso(usuario, false).empresaId(), id); }
    public Carteira carteira(UUID usuario, UUID id, int pagina, int tamanho) {
        var a = acesso(usuario, false); pagina(pagina, tamanho);
        parceiros.buscar(a.empresaId(), id);
        if (!a.modulos().contains("contatos")) throw new AccessDeniedException("Você não tem acesso à carteira de contatos.");
        var itens = jdbc.query("SELECT id,nome,tipo FROM contato WHERE empresa_id=? AND parceiro_id=? ORDER BY LOWER(nome),id LIMIT ? OFFSET ?",
                (rs, n) -> new Cliente(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3)), a.empresaId(), id, tamanho, pagina * tamanho);
        var total = jdbc.queryForObject("SELECT COUNT(*) FROM contato WHERE empresa_id=? AND parceiro_id=?", Long.class, a.empresaId(), id);
        return new Carteira(itens, total, pagina, tamanho);
    }
    private Contato exigirVersao(UUID empresa, UUID id, Integer versao) {
        parceiros.buscar(empresa, id);
        var atual = contatos.buscar(empresa, id);
        if (versao == null || versao != atual.versao()) throw new ConflitoException("Parceiro atualizado em outra sessão. Reabra a ficha antes de alterar.");
        return atual;
    }
    @Transactional
    public Parceiro salvar(UUID usuario, UUID id, Formulario f) {
        var empresa = acesso(usuario, true).empresaId(); contatos.bloquear(empresa);
        var atual = id == null ? null : exigirVersao(empresa, id, f.versao());
        var parceiro = new Parceiro(id == null ? UUID.randomUUID() : id, empresa, atual == null ? 0 : atual.versao() + 1, f.dados(), f.socios(), f.areasAtuacao());
        var dados = new HashMap<String, String>(atual == null ? Map.of("tipo", "Parceiro", "whatsapp", "nenhum", "carteira", "casa") : atual.dados());
        Parceiro.COMUNS.forEach(k -> dados.put(k, parceiro.dados().get(k)));
        var contato = new Contato(parceiro.id(), empresa, parceiro.versao(), dados, atual == null ? List.of() : atual.representantes(), atual == null ? List.of() : atual.indicadores());
        contatos.salvar(contato, atual == null); parceiros.salvar(parceiro);
        contatos.auditar(empresa, parceiro.id(), usuario, atual == null ? "CRIAR" : "EDITAR");
        return parceiros.buscar(empresa, parceiro.id());
    }
    @Transactional
    public void excluir(UUID usuario, UUID id, Integer versao) {
        var empresa = acesso(usuario, true).empresaId(); contatos.bloquear(empresa); exigirVersao(empresa, id, versao);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM contato WHERE empresa_id=? AND parceiro_id=?", Long.class, empresa, id) > 0)
            throw new ConflitoException("Este parceiro possui contatos na carteira. Remova os vínculos antes de excluir.");
        contatos.excluir(empresa, id); contatos.auditar(empresa, id, usuario, "EXCLUIR");
    }
}
