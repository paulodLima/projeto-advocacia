package com.advocacia_microservice.documento.application;
import com.advocacia_microservice.documento.domain.PapelTimbrado;
import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.PapelEmpresa;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly = true)
public class DocumentosConfigService {
    public record Config(UUID empresaId, PapelEmpresa papel, PapelTimbrado documento, Map<String, String> modelos) {}
    public static final Set<String> CHAVES = Set.of("recibo:corpo", "recibo:rodape", "fatura:abertura", "fatura:rodape", "demonstrativo:abertura_reembolsar", "demonstrativo:abertura_completo", "demonstrativo:rodape", "prestacao:abertura", "prestacao:rodape");
    private final EmpresaService empresas; private final JdbcTemplate jdbc;
    public DocumentosConfigService(EmpresaService empresas, JdbcTemplate jdbc) { this.empresas = empresas; this.jdbc = jdbc; }
    public Config buscar(UUID usuario) {
        var empresa = empresas.buscar(usuario);
        if (empresa.id() == null) return new Config(null, null, PapelTimbrado.padrao(), Map.of());
        var papel = jdbc.query("SELECT * FROM documento_config WHERE empresa_id=?", (rs, n) -> new PapelTimbrado(rs.getString("logo"), rs.getString("rodape"), rs.getString("alinhamento"), rs.getBoolean("repetir"), rs.getBoolean("documento"), rs.getBoolean("endereco"), rs.getBoolean("contato")), empresa.id()).stream().findFirst().orElseGet(PapelTimbrado::padrao);
        var modelos = new HashMap<String, String>();
        jdbc.query("SELECT chave,texto FROM documento_modelo WHERE empresa_id=?", rs -> { modelos.put(rs.getString("chave"), rs.getString("texto")); }, empresa.id());
        return new Config(empresa.id(), empresa.papel(), papel, modelos);
    }
    private UUID editar(UUID usuario) { UUID id = empresas.exigirMaster(usuario).empresaId(); jdbc.queryForObject("SELECT id FROM empresa WHERE id=? FOR UPDATE", UUID.class, id); return id; }
    @Transactional public PapelTimbrado salvarPapel(UUID usuario, PapelTimbrado papel) {
        UUID id = editar(usuario); var p = papel.normalizado();
        jdbc.update("DELETE FROM documento_config WHERE empresa_id=?", id);
        jdbc.update("INSERT INTO documento_config(empresa_id,logo,rodape,alinhamento,repetir,documento,endereco,contato) VALUES (?,?,?,?,?,?,?,?)", id, p.logo(), p.rodape(), p.alinhamento(), p.repetir(), p.documento(), p.endereco(), p.contato()); return p;
    }
    @Transactional public Map<String, String> salvarModelos(UUID usuario, Map<String, String> modelos) {
        UUID id = editar(usuario);
        if (modelos == null || !CHAVES.containsAll(modelos.keySet())) throw new IllegalArgumentException("Modelo de documento inválido.");
        var seguros = new HashMap<String, String>();
        modelos.forEach((chave, texto) -> {
            if (texto == null || texto.length() > 20000) throw new IllegalArgumentException("Cada trecho deve ter até 20.000 caracteres.");
            var html = Jsoup.parseBodyFragment(texto);
            html.select("[style]").forEach(elemento -> {
                String style = elemento.attr("style").strip().toLowerCase(Locale.ROOT);
                if (!style.matches("text-align\\s*:\\s*(left|center|right|justify)\\s*;?")) elemento.removeAttr("style");
            });
            var whitelist = new Safelist().addTags("p", "div", "span", "br", "b", "strong", "i", "em", "u", "ul", "ol", "li").addAttributes("p", "style").addAttributes("div", "style");
            seguros.put(chave, Jsoup.clean(html.body().html(), "", whitelist, new org.jsoup.nodes.Document.OutputSettings().prettyPrint(false)));
        });
        jdbc.update("DELETE FROM documento_modelo WHERE empresa_id=?", id);
        seguros.forEach((chave, texto) -> jdbc.update("INSERT INTO documento_modelo(empresa_id,chave,texto) VALUES (?,?,?)", id, chave, texto));
        return Map.copyOf(seguros);
    }
}
