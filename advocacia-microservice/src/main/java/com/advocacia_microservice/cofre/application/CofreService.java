package com.advocacia_microservice.cofre.application;

import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.PapelEmpresa;
import com.advocacia_microservice.equipe.application.EquipeService;
import com.advocacia_microservice.equipe.domain.PerfilEquipe;
import com.advocacia_microservice.shared.exception.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional(readOnly=true)
public class CofreService {
    public record Formulario(String nome,String url,String usuario,String senha,String descricao,String observacao,boolean soAdmin,Integer versao) {
        @Override public String toString() { return "Formulario[credencial]"; }
    }
    public record Credencial(UUID id,String nome,String url,String usuario,String descricao,String observacao,boolean soAdmin,int versao) {}
    public record Dados(UUID empresaId,boolean podeEditar,boolean configurado,List<Credencial> credenciais) {}
    private record Registro(UUID id,String nome,String url,boolean soAdmin,int versao,String conteudo) {}
    private record Conteudo(String usuario,String senha,String descricao,String observacao) {}
    private final JdbcTemplate jdbc;private final EmpresaService empresas;private final EquipeService equipe;private final CofreCrypto crypto;
    public CofreService(JdbcTemplate jdbc,EmpresaService empresas,EquipeService equipe,CofreCrypto crypto) { this.jdbc=jdbc;this.empresas=empresas;this.equipe=equipe;this.crypto=crypto; }
    private boolean admin(UUID usuario) { return equipe.acesso(usuario).perfil()==PerfilEquipe.ADMINISTRADOR; }
    private List<Registro> registros(UUID empresa) { return jdbc.query("SELECT * FROM cofre_credencial WHERE empresa_id=? ORDER BY nome_chave,id",(rs,n)->new Registro(rs.getObject("id",UUID.class),rs.getString("nome"),rs.getString("url"),rs.getBoolean("so_admin"),rs.getInt("versao"),rs.getString("conteudo")),empresa); }
    public Dados buscar(UUID usuario) {
        var empresa=empresas.buscar(usuario);
        if(empresa.id()==null) return new Dados(null,false,crypto.configurado(),List.of());
        if(!crypto.configurado()) return new Dados(empresa.id(),empresa.papel()==PapelEmpresa.MASTER,false,List.of());
        boolean admin=admin(usuario);
        return new Dados(empresa.id(),empresa.papel()==PapelEmpresa.MASTER,true,registros(empresa.id()).stream().filter(r->!r.soAdmin()||admin).map(r->publico(empresa.id(),r)).toList());
    }
    private Registro exigir(UUID empresa,UUID id) { return registros(empresa).stream().filter(r->r.id().equals(id)).findFirst().orElseThrow(()->new RecursoNaoEncontradoException("Credencial não encontrada.")); }
    private Credencial publico(UUID empresa,Registro r) { var c=ler(empresa,r);return new Credencial(r.id(),r.nome(),r.url(),c.usuario(),c.descricao(),c.observacao(),r.soAdmin(),r.versao()); }
    private UUID editar(UUID usuario) { UUID id=empresas.exigirMaster(usuario).empresaId();jdbc.queryForObject("SELECT id FROM empresa WHERE id=? FOR UPDATE",UUID.class,id);return id; }
    private void versao(Registro r,Integer versao) { if(versao==null||versao!=r.versao()) throw new ConflitoException("Credencial atualizada em outra sessão. Recarregue o cofre antes de alterar."); }
    private String texto(String s,int max) { if(s==null||s.length()>max) throw new IllegalArgumentException("Verifique os limites dos campos da credencial.");return s; }
    @Transactional public Credencial salvar(UUID usuario,UUID id,Formulario f) {
        UUID empresa=editar(usuario);Registro anterior=id==null ? null : exigir(empresa,id);
        if(anterior!=null) versao(anterior,f.versao());
        String nome=texto(f.nome(),150).strip();if(nome.isEmpty()) throw new IllegalArgumentException("Informe o nome do sistema.");
        String url=texto(f.url(),1000).strip();
        if(!url.isEmpty()) { try { var u=URI.create(url);if(!Set.of("http","https").contains(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null) throw new IllegalArgumentException(); }catch(Exception e) { throw new IllegalArgumentException("Informe um endereço HTTP ou HTTPS sem credenciais na URL."); } }
        String senha=f.senha();
        if(anterior!=null && senha==null) senha=ler(empresa,anterior).senha();
        if(senha==null||senha.isEmpty()||senha.length()>4096) throw new IllegalArgumentException("Informe uma senha de até 4096 caracteres.");
        var conteudo=new Conteudo(texto(f.usuario(),254),senha,texto(f.descricao(),500),texto(f.observacao(),4000));
        UUID alvo=id==null ? UUID.randomUUID() : id;String cifrado=gravar(empresa,alvo,conteudo);
        if(id==null) jdbc.update("INSERT INTO cofre_credencial(id,empresa_id,nome,nome_chave,url,so_admin,versao,conteudo) VALUES (?,?,?,?,?,?,0,?)",alvo,empresa,nome,nome.toLowerCase(Locale.ROOT),url,f.soAdmin(),cifrado);
        else jdbc.update("UPDATE cofre_credencial SET nome=?,nome_chave=?,url=?,so_admin=?,versao=versao+1,conteudo=? WHERE empresa_id=? AND id=?",nome,nome.toLowerCase(Locale.ROOT),url,f.soAdmin(),cifrado,empresa,id);
        return new Credencial(alvo,nome,url,conteudo.usuario(),conteudo.descricao(),conteudo.observacao(),f.soAdmin(),anterior==null ? 0 : anterior.versao()+1);
    }
    public String revelar(UUID usuario,UUID id) {
        var empresa=empresas.buscar(usuario);if(empresa.id()==null) throw new RecursoNaoEncontradoException("Credencial não encontrada.");
        var r=exigir(empresa.id(),id);if(r.soAdmin()&&!admin(usuario)) throw new RecursoNaoEncontradoException("Credencial não encontrada.");
        return ler(empresa.id(),r).senha();
    }
    @Transactional public void excluir(UUID usuario,UUID id,Integer versao) { UUID empresa=editar(usuario);var r=exigir(empresa,id);versao(r,versao);jdbc.update("DELETE FROM cofre_credencial WHERE empresa_id=? AND id=?",empresa,id); }
    private String gravar(UUID empresa,UUID id,Conteudo c) {
        try { var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeUTF(c.usuario());out.writeUTF(c.senha());out.writeUTF(c.descricao());out.writeUTF(c.observacao());return crypto.cifrar(empresa,id,bytes.toByteArray()); }
        catch(IOException e) { throw new IllegalArgumentException("Credencial inválida."); }
    }
    private Conteudo ler(UUID empresa,Registro r) {
        try { var in=new DataInputStream(new ByteArrayInputStream(crypto.decifrar(empresa,r.id(),r.conteudo())));return new Conteudo(in.readUTF(),in.readUTF(),in.readUTF(),in.readUTF()); }
        catch(IOException e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Não foi possível abrir o cofre."); }
    }
}
