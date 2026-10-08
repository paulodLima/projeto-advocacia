package com.advocacia_microservice.cliente.application;

import com.advocacia_microservice.cliente.domain.Contato;
import com.advocacia_microservice.cliente.infrastructure.persistence.ContatoRepository;
import com.advocacia_microservice.equipe.application.EquipeService;
import com.advocacia_microservice.equipe.domain.PerfilEquipe;
import com.advocacia_microservice.empresa.domain.PapelEmpresa;
import com.advocacia_microservice.shared.exception.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class ContatosService {
    public record Formulario(Integer versao,Map<String,String> dados,List<Map<String,String>> representantes,List<UUID> indicadores) {}
    public record Indicador(UUID id,String nome,String cor) {}
    public record Opcao(UUID id,String nome) {}
    public record Pagina(List<Contato> itens,long total,int pagina,int tamanho) {}
    public record Opcoes(boolean podeEditar,List<Indicador> indicadores,List<Opcao> origens,List<Opcao> parceiros) {}
    private final ContatoRepository repository;private final EquipeService equipe;private final JdbcTemplate jdbc;
    public ContatosService(ContatoRepository repository,EquipeService equipe,JdbcTemplate jdbc) {this.repository=repository;this.equipe=equipe;this.jdbc=jdbc;}
    private EquipeService.Acesso acesso(UUID usuario,boolean escrita) {
        var a=equipe.acesso(usuario);
        if(a.empresaId()==null||!a.modulos().contains("contatos")||!equipe.ativo(usuario)) throw new AccessDeniedException("Você não tem acesso aos contatos desta empresa.");
        if(escrita&&!edita(a)) throw new AccessDeniedException("Seu perfil permite apenas consultar contatos.");return a;
    }
    private boolean edita(EquipeService.Acesso a) {return a.papel()==PapelEmpresa.MASTER || Set.of(PerfilEquipe.ADMINISTRADOR,PerfilEquipe.ADVOGADO,PerfilEquipe.ASSISTENTE).contains(a.perfil());}
    private List<Indicador> indicadores(UUID empresa) {return jdbc.query("SELECT id,nome,cor FROM contato_indicador WHERE empresa_id=? ORDER BY nome_chave,id",(rs,n)->new Indicador(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3)),empresa);}
    private List<Opcao> opcoes(UUID empresa,String tipo) {return repository.opcoes(empresa,tipo).stream().map(m->new Opcao((UUID)m.get("id"),(String)m.get("nome"))).toList();}
    public Opcoes opcoes(UUID usuario) {var a=acesso(usuario,false);return new Opcoes(edita(a),indicadores(a.empresaId()),opcoes(a.empresaId(),"origens"),opcoes(a.empresaId(),"parceiros"));}
    public Pagina listar(UUID usuario,String busca,String tipo,UUID indicador,int pagina,int tamanho) {
        var empresa=acesso(usuario,false).empresaId();
        if(busca==null||busca.length()>150||tipo==null||(!tipo.isEmpty()&&!Contato.TIPOS.contains(tipo))||pagina<0||pagina>1000000||tamanho<1||tamanho>100) throw new IllegalArgumentException("Filtros ou paginação inválidos.");
        return new Pagina(repository.carregarPagina(empresa,repository.ids(empresa,busca.strip(),tipo,indicador,pagina,tamanho)),repository.total(empresa,busca.strip(),tipo,indicador),pagina,tamanho);
    }
    public Contato buscar(UUID usuario,UUID id) {return repository.buscar(acesso(usuario,false).empresaId(),id);}
    private Contato exigirVersao(UUID empresa,UUID id,Integer versao) {var c=repository.buscar(empresa,id);if(versao==null||versao!=c.versao()) throw new ConflitoException("Contato atualizado em outra sessão. Reabra a ficha antes de alterar.");return c;}
    @Transactional public Contato salvar(UUID usuario,UUID id,Formulario f) {
        var empresa=acesso(usuario,true).empresaId();repository.bloquear(empresa);
        var anterior=id==null?null:exigirVersao(empresa,id,f.versao());
        var c=new Contato(id==null?UUID.randomUUID():id,empresa,anterior==null?0:anterior.versao()+1,f.dados(),f.representantes(),f.indicadores());
        UUID origem=Contato.uuid(c.dados().get("origem_id")), parceiro=Contato.uuid(c.dados().get("carteira_parceiro_id"));
        if(origem!=null&&!repository.referencia(empresa,origem,"cadastro_item","origens")) throw new IllegalArgumentException("Selecione uma origem ativa da sua empresa.");
        if(parceiro!=null&&(parceiro.equals(c.id())||!repository.referencia(empresa,parceiro,"contato","Parceiro"))) throw new IllegalArgumentException("Selecione um parceiro da sua empresa.");
        var validos=indicadores(empresa).stream().map(Indicador::id).toList();if(!validos.containsAll(c.indicadores())) throw new IllegalArgumentException("Indicador fora da sua empresa.");
        if(anterior!=null&&anterior.dados().get("tipo").equals("Parceiro")&&!c.dados().get("tipo").equals("Parceiro")&&jdbc.queryForObject("SELECT COUNT(*) FROM contato WHERE empresa_id=? AND parceiro_id=?",Long.class,empresa,id)>0) throw new ConflitoException("Este parceiro possui contatos vinculados à carteira.");
        repository.salvar(c,anterior==null);repository.auditar(empresa,c.id(),usuario,anterior==null?"CRIAR":"EDITAR");return c;
    }
    @Transactional public void excluir(UUID usuario,UUID id,Integer versao) {var empresa=acesso(usuario,true).empresaId();repository.bloquear(empresa);exigirVersao(empresa,id,versao);if(jdbc.queryForObject("SELECT COUNT(*) FROM contato WHERE empresa_id=? AND parceiro_id=?",Long.class,empresa,id)>0) throw new ConflitoException("Contato vinculado a uma carteira. Remova o vínculo antes de excluir.");repository.excluir(empresa,id);repository.auditar(empresa,id,usuario,"EXCLUIR");}
    @Transactional public Indicador criarIndicador(UUID usuario,String nome) {
        var empresa=acesso(usuario,true).empresaId();repository.bloquear(empresa);
        if(nome==null||nome.isBlank()||nome.strip().length()>80) throw new IllegalArgumentException("Informe um indicador de até 80 caracteres.");
        var chave=nome.strip().toLowerCase(Locale.ROOT);var atuais=indicadores(empresa);var existente=atuais.stream().filter(i->i.nome().toLowerCase(Locale.ROOT).equals(chave)).findFirst();if(existente.isPresent()) return existente.get();
        if(atuais.size()>=100) throw new IllegalArgumentException("Limite de 100 indicadores por empresa.");
        var cores=List.of("#6A7662","#9A8271","#B0B796","#C9A66B","#8a8275","#7d8f6f");var i=new Indicador(UUID.randomUUID(),nome.strip(),cores.get(atuais.size()%cores.size()));
        jdbc.update("INSERT INTO contato_indicador(id,empresa_id,nome,nome_chave,cor) VALUES (?,?,?,?,?)",i.id(),empresa,i.nome(),chave,i.cor());return i;
    }
}
