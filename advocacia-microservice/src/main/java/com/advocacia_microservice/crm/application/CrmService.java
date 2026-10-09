package com.advocacia_microservice.crm.application;

import com.advocacia_microservice.crm.domain.Lead;
import com.advocacia_microservice.crm.infrastructure.CrmRepository;
import com.advocacia_microservice.cliente.domain.Contato;
import com.advocacia_microservice.cliente.infrastructure.persistence.ContatoRepository;
import com.advocacia_microservice.equipe.application.EquipeService;
import com.advocacia_microservice.equipe.domain.PerfilEquipe;
import com.advocacia_microservice.empresa.domain.PapelEmpresa;
import com.advocacia_microservice.shared.exception.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class CrmService {
    public record Formulario(Integer versao,Lead.Cadastro cadastro) {}
    public record Atualizacao(Integer versao,Lead.Estado estado) {}
    public record ContatoRequest(Integer versao,LocalDate data,String canal,String resultado,String observacao) {}
    public record ComentarioRequest(Integer versao,String texto,List<UUID> mencoes) {}
    public record ConversaoRequest(Integer versao,boolean criarCaso) {}
    public record Opcao(UUID id,String nome,String cor) {}
    public record Opcoes(boolean podeEditar,boolean podeConverter,List<String> origens,List<Opcao> etiquetas,List<Opcao> membros,List<Lead.Passo> cadencia) {}
    public record Indicadores(long emCadencia,long atrasados,long quentes,long ganhos,long decididos,Integer conversao) {}
    public record Pagina(List<Lead> itens,long total,int pagina,int tamanho,Indicadores indicadores,List<String> origens) {}
    public record Campanha(UUID id,int versao,String nome,Integer mes,String cor,String descricao,List<UUID> etiquetas,List<Destinatario> destinatarios) {}
    public record Destinatario(String chave,String tipo,String nome,String contato,boolean abordado) {}
    public record CampanhaRequest(Integer versao,String nome,Integer mes,String cor,String descricao,List<UUID> etiquetas) {}
    public record AbordagemRequest(String destinatario,int ano,boolean abordado) {}
    public record Marketing(List<Campanha> campanhas,List<Map<String,Object>> manutencoes,int ano) {}
    private final CrmRepository repo; private final JdbcTemplate jdbc; private final EquipeService equipe; private final ContatoRepository contatos;
    private static final ZoneId ZONA=ZoneId.of("America/Sao_Paulo");
    public CrmService(CrmRepository repo,JdbcTemplate jdbc,EquipeService equipe,ContatoRepository contatos) {this.repo=repo;this.jdbc=jdbc;this.equipe=equipe;this.contatos=contatos;}
    private boolean edita(EquipeService.Acesso a) {return a.papel()==PapelEmpresa.MASTER || Set.of(PerfilEquipe.ADMINISTRADOR,PerfilEquipe.ADVOGADO,PerfilEquipe.ASSISTENTE).contains(a.perfil());}
    private EquipeService.Acesso acesso(UUID usuario,boolean escrita) {
        var a=equipe.acesso(usuario);
        if(a.empresaId()==null || !a.modulos().contains("crm") || !equipe.ativo(usuario)) throw new AccessDeniedException("Você não tem acesso ao CRM desta empresa.");
        if(escrita && !edita(a)) throw new AccessDeniedException("Seu perfil permite apenas consultar o CRM."); return a;
    }
    private List<Opcao> etiquetas(UUID empresa) {
        return jdbc.query("SELECT i.id,i.nome,COALESCE(c.valor,'#7d6c5e') AS cor FROM cadastro_item i LEFT JOIN cadastro_campo c ON c.item_id=i.id AND c.campo='cor' WHERE i.empresa_id=? AND i.tipo='etiquetas' AND i.ativo=true ORDER BY i.nome",(r,n)->new Opcao(r.getObject(1,UUID.class),r.getString(2),r.getString(3)),empresa);
    }
    private List<Opcao> membros(UUID empresa) {
        return jdbc.query("SELECT u.id,u.nome FROM empresa_usuario v JOIN usuario u ON u.id=v.usuario_id LEFT JOIN equipe_membro m ON m.usuario_id=u.id WHERE v.empresa_id=? AND u.status='ATIVO' AND (m.situacao IS NULL OR m.situacao='ATIVO') ORDER BY u.nome",(r,n)->new Opcao(r.getObject(1,UUID.class),r.getString(2),""),empresa).stream().filter(m->equipe.acesso(m.id()).modulos().contains("crm")).toList();
    }
    public Opcoes opcoes(UUID usuario) {var a=acesso(usuario,false);return new Opcoes(edita(a),edita(a)&&a.modulos().containsAll(Set.of("contatos","casos")),Lead.ORIGENS,etiquetas(a.empresaId()),membros(a.empresaId()),repo.cadencia(a.empresaId()));}
    private void validarEtiquetas(UUID empresa,List<UUID> ids,List<UUID> anteriores) {
        var permitidos=new HashSet<>(etiquetas(empresa).stream().map(Opcao::id).toList());permitidos.addAll(anteriores);
        if(!permitidos.containsAll(ids)) throw new IllegalArgumentException("Selecione etiquetas da sua empresa.");
    }
    public Pagina listar(UUID usuario,String busca,String status,String temperatura,String origem,boolean atrasados,int pagina,int tamanho) {
        var empresa=acesso(usuario,false).empresaId();
        if(busca==null || busca.length()>150 || origem==null || origem.length()>150 || pagina<0 || pagina>1000000 || tamanho<1 || tamanho>100 || !Set.of("ativo","agendou","ganho","perdido","todos").contains(status) || !Set.of("","quente","morno","frio").contains(temperatura)) throw new IllegalArgumentException("Filtros inválidos.");
        var filtro=repo.filtro(empresa,busca,status,temperatura,origem,atrasados);
        long ativos=contar(empresa,"status='ativo'"), quentes=contar(empresa,"status='ativo' AND temperatura='quente'"), ganhos=contar(empresa,"status='ganho'"), decididos=contar(empresa,"status IN ('ganho','perdido')");
        var vencidos=jdbc.queryForObject("SELECT COUNT(*) FROM crm_lead WHERE empresa_id=? AND status='ativo' AND proximo<=?",Long.class,empresa,LocalDate.now(ZONA));
        return new Pagina(repo.carregar(empresa,repo.ids(filtro,pagina,tamanho)),repo.total(filtro),pagina,tamanho,new Indicadores(ativos,vencidos,quentes,ganhos,decididos,decididos==0?null:(int)Math.round(ganhos*100.0/decididos)),jdbc.queryForList("SELECT DISTINCT origem FROM crm_lead WHERE empresa_id=? AND origem<>'' ORDER BY origem",String.class,empresa));
    }
    private long contar(UUID empresa,String condicao) {return jdbc.queryForObject("SELECT COUNT(*) FROM crm_lead WHERE empresa_id=? AND "+condicao,Long.class,empresa);}
    public Lead buscar(UUID usuario,UUID id) {return repo.buscar(acesso(usuario,false).empresaId(),id);}
    private Lead versao(UUID empresa,UUID id,Integer versao) {
        var lead=repo.buscar(empresa,id);if(versao==null || versao!=lead.versao()) throw new ConflitoException("Lead atualizado em outra sessão. Recarregue antes de alterar.");return lead;
    }
    @Transactional public Lead salvar(UUID usuario,UUID id,Formulario f) {
        var empresa=acesso(usuario,true).empresaId();repo.bloquear(empresa);
        if(f==null || f.cadastro()==null) throw new IllegalArgumentException("Informe os dados do lead.");
        var l=id==null?null:versao(empresa,id,f.versao()); var c=f.cadastro();
        validarEtiquetas(empresa,c.etiquetas(),l==null?List.of():l.dados().cadastro().etiquetas());
        var d=l==null?new Lead.Dados(c,Lead.Estado.inicial(),LocalDate.now(ZONA),null,0,null):new Lead.Dados(c,l.dados().estado(),l.dados().entrada(),l.dados().ultimoContato(),l.dados().passo(),l.dados().casoId());
        var leadId=id==null?UUID.randomUUID():id;repo.salvar(empresa,leadId,l==null?0:l.versao()+1,d,l==null);sincronizarConsulta(empresa,leadId,d);return repo.buscar(empresa,leadId);
    }
    @Transactional public Lead estado(UUID usuario,UUID id,Atualizacao f) {
        var empresa=acesso(usuario,true).empresaId();repo.bloquear(empresa);var l=versao(empresa,id,f.versao());
        if(f.estado()==null) throw new IllegalArgumentException("Informe o estado do lead.");
        var d=new Lead.Dados(l.dados().cadastro(),f.estado(),l.dados().entrada(),l.dados().ultimoContato(),l.dados().passo(),l.dados().casoId());
        repo.salvar(empresa,id,l.versao()+1,d,false);sincronizarConsulta(empresa,id,d);return repo.buscar(empresa,id);
    }
    private void sincronizarConsulta(UUID empresa,UUID id,Lead.Dados d) {
        if(!d.estado().consultaAgendada() || d.estado().consultaEm()==null) {jdbc.update("DELETE FROM agenda_evento WHERE empresa_id=? AND lead_id=?",empresa,id);return;}
        var titulo="Consulta — "+d.cadastro().nome();
        if(jdbc.update("UPDATE agenda_evento SET titulo=?,quando=?,status=? WHERE empresa_id=? AND lead_id=?",titulo,d.estado().consultaEm(),d.estado().consultaStatus(),empresa,id)==0)
            jdbc.update("INSERT INTO agenda_evento(id,empresa_id,lead_id,titulo,quando,status) VALUES (?,?,?,?,?,?)",UUID.randomUUID(),empresa,id,titulo,d.estado().consultaEm(),d.estado().consultaStatus());
    }
    @Transactional public Lead registrar(UUID usuario,UUID id,ContatoRequest f) {
        var empresa=acesso(usuario,true).empresaId();repo.bloquear(empresa);var l=versao(empresa,id,f.versao());
        if(!l.dados().estado().status().equals("ativo")) throw new ConflitoException("Reative o lead antes de registrar uma tentativa de contato.");
        if(f.data()==null || f.data().isAfter(LocalDate.now(ZONA)) || f.data().isBefore(l.dados().entrada()) || (l.dados().ultimoContato()!=null && f.data().isBefore(l.dados().ultimoContato())) || !Lead.CANAIS.contains(f.canal()) || !Lead.RESULTADOS.contains(f.resultado())) throw new IllegalArgumentException("Confira data, canal e resultado do contato.");
        jdbc.update("INSERT INTO crm_tratativa(id,empresa_id,lead_id,data,canal,resultado,observacao,autor_id) VALUES (?,?,?,?,?,?,?,?)",UUID.randomUUID(),empresa,id,f.data(),f.canal(),f.resultado(),Lead.texto(f.observacao(),4000,false),usuario);
        String status=f.resultado().equals("Quer agendar consultoria")?"agendou":f.resultado().equals("Sem interesse")?"perdido":l.dados().estado().status();
        var e=l.dados().estado();var novo=new Lead.Estado(status,false,null,e.consultaAgendada(),e.consultaEm(),e.consultaStatus(),e.qualificacao());
        var passo=status.equals("ativo")?Math.min(l.dados().passo()+1,repo.cadencia(empresa).size()):l.dados().passo();
        repo.salvar(empresa,id,l.versao()+1,new Lead.Dados(l.dados().cadastro(),novo,l.dados().entrada(),f.data(),passo,l.dados().casoId()),false);return repo.buscar(empresa,id);
    }
    @Transactional public Lead comentar(UUID usuario,UUID id,ComentarioRequest f) {
        var empresa=acesso(usuario,true).empresaId();repo.bloquear(empresa);var l=versao(empresa,id,f.versao());
        var texto=Lead.texto(f.texto(),4000,true);var mencoes=Lead.ids(f.mencoes());
        if(!membros(empresa).stream().map(Opcao::id).toList().containsAll(mencoes)) throw new IllegalArgumentException("Mencione membros ativos com acesso ao CRM desta empresa.");
        var comentario=UUID.randomUUID();jdbc.update("INSERT INTO crm_comentario(id,empresa_id,lead_id,texto,autor_id) VALUES (?,?,?,?,?)",comentario,empresa,id,texto,usuario);
        for(var membro:mencoes){jdbc.update("INSERT INTO crm_comentario_mencao(comentario_id,usuario_id) VALUES (?,?)",comentario,membro);if(!membro.equals(usuario)) jdbc.update("INSERT INTO crm_notificacao(id,empresa_id,usuario_id,comentario_id) VALUES (?,?,?,?)",UUID.randomUUID(),empresa,membro,comentario);}
        repo.salvar(empresa,id,l.versao()+1,l.dados(),false);return repo.buscar(empresa,id);
    }
    @Transactional public void excluir(UUID usuario,UUID id,int versao) {
        var empresa=acesso(usuario,true).empresaId();repo.bloquear(empresa);var l=versao(empresa,id,versao);
        if(l.dados().casoId()!=null) throw new ConflitoException("O lead possui caso vinculado. Preserve seu histórico.");
        jdbc.update("DELETE FROM crm_lead WHERE empresa_id=? AND id=?",empresa,id);
    }
    @Transactional public Lead converter(UUID usuario,UUID id,ConversaoRequest f) {
        var a=acesso(usuario,true);var empresa=a.empresaId();repo.bloquear(empresa);var l=repo.buscar(empresa,id);
        if(l.dados().casoId()!=null && f.criarCaso()) return l;
        l=versao(empresa,id,f.versao());UUID casoId=l.dados().casoId();
        if(f.criarCaso()) {
            if(!a.modulos().containsAll(Set.of("contatos","casos"))) throw new AccessDeniedException("A conversão exige acesso a Contatos e Casos.");
            var c=l.dados().cadastro();
            var emailIds=c.email().isBlank()?List.<UUID>of():jdbc.queryForList("SELECT c.id FROM contato c JOIN contato_dado d ON d.contato_id=c.id WHERE c.empresa_id=? AND c.tipo='Cliente' AND d.campo='email' AND LOWER(d.valor)=?",UUID.class,empresa,c.email());
            var telefone=c.telefone().replaceAll("\\D","");
            var telefoneIds=jdbc.query("SELECT c.id,d.valor FROM contato c JOIN contato_dado d ON d.contato_id=c.id WHERE c.empresa_id=? AND c.tipo='Cliente' AND d.campo='telefone'",(r,n)->Map.entry(r.getObject(1,UUID.class),r.getString(2)),empresa).stream().filter(v->v.getValue().replaceAll("\\D","").equals(telefone)).map(Map.Entry::getKey).toList();
            var candidatos=new HashSet<>(emailIds);candidatos.addAll(telefoneIds);
            if(candidatos.size()>1) throw new ConflitoException("E-mail e telefone correspondem a clientes diferentes. Confira os contatos antes de converter.");
            UUID cliente=candidatos.stream().findFirst().orElse(null);
            if(cliente==null) {
                cliente=UUID.randomUUID();var dados=new HashMap<String,String>();dados.put("nome",c.nome());dados.put("telefone",c.telefone());dados.put("email",c.email());dados.put("observacoes",c.observacoes());dados.put("tipo","Cliente");dados.put("tipo_pessoa","PF");dados.put("whatsapp","telefone");dados.put("carteira","casa");
                contatos.salvar(new Contato(cliente,empresa,0,dados,List.of(),List.of(),true),true);contatos.auditar(empresa,cliente,usuario,"CRIAR");
            }
            casoId=UUID.randomUUID();jdbc.update("INSERT INTO caso(id,empresa_id,cliente_id,lead_id,titulo,observacoes,historico) VALUES (?,?,?,?,?,?,?)",casoId,empresa,cliente,id,c.necessidade()+" — "+c.nome(),c.observacoes(),repo.json(Map.of("entrada",l.dados().entrada(),"origem",c.origem(),"necessidade",c.necessidade(),"contatos",l.contatos(),"comentarios",l.comentarios())));
        }
        var d=l.dados();repo.salvar(empresa,id,l.versao()+1,new Lead.Dados(d.cadastro(),d.estado().comStatus("ganho"),d.entrada(),d.ultimoContato(),d.passo(),casoId),false);return repo.buscar(empresa,id);
    }
    @Transactional public List<Lead.Passo> salvarCadencia(UUID usuario,List<Lead.Passo> passos) {
        var a=acesso(usuario,true);if(a.papel()!=PapelEmpresa.MASTER) throw new AccessDeniedException("Somente o master configura a cadência.");
        if(passos==null || passos.isEmpty() || passos.size()>20) throw new IllegalArgumentException("Informe de 1 a 20 passos.");
        for(int i=0;i<passos.size();i++) if(passos.get(i)==null || passos.get(i).passo()!=i) throw new IllegalArgumentException("Passos devem ser sequenciais, começando em zero.");
        repo.bloquear(a.empresaId());jdbc.update("DELETE FROM crm_cadencia WHERE empresa_id=?",a.empresaId());
        for(var p:passos) jdbc.update("INSERT INTO crm_cadencia(empresa_id,passo,dias,rotulo) VALUES (?,?,?,?)",a.empresaId(),p.passo(),p.dias(),p.rotulo());
        var ids=jdbc.queryForList("SELECT id FROM crm_lead WHERE empresa_id=?",UUID.class,a.empresaId());
        for(int i=0;i<ids.size();i+=100) for(var l:repo.carregar(a.empresaId(),ids.subList(i,Math.min(i+100,ids.size())))) repo.salvar(a.empresaId(),l.id(),l.versao()+1,l.dados(),false);
        return passos;
    }
    private List<UUID> campanhaTags(UUID empresa,UUID id) {return jdbc.queryForList("SELECT etiqueta_id FROM crm_campanha_tag WHERE empresa_id=? AND campanha_id=?",UUID.class,empresa,id);}
    private int campanhaVersao(UUID empresa,UUID id) {var v=jdbc.queryForList("SELECT versao FROM crm_campanha WHERE empresa_id=? AND id=?",Integer.class,empresa,id);if(v.isEmpty()) throw new RecursoNaoEncontradoException("Campanha não encontrada.");return v.getFirst();}
    @Transactional public void salvarCampanha(UUID usuario,UUID id,CampanhaRequest f) {
        var empresa=acesso(usuario,true).empresaId();repo.bloquear(empresa);boolean novo=id==null;
        var nome=Lead.texto(f.nome(),150,true);var descricao=Lead.texto(f.descricao(),500,false);
        if(f.cor()==null || !f.cor().matches("#[0-9a-fA-F]{6}") || (f.mes()!=null && (f.mes()<1 || f.mes()>12))) throw new IllegalArgumentException("Cor ou mês inválido.");
        var tags=Lead.ids(f.etiquetas());validarEtiquetas(empresa,tags,novo?List.of():campanhaTags(empresa,id));
        int v=novo?0:campanhaVersao(empresa,id)+1;if(!novo && !Objects.equals(f.versao(),v-1)) throw new ConflitoException("Campanha alterada em outra sessão.");
        if(novo) {id=UUID.randomUUID();jdbc.update("INSERT INTO crm_campanha(id,empresa_id,versao,nome,mes,cor,descricao) VALUES (?,?,?,?,?,?,?)",id,empresa,v,nome,f.mes(),f.cor(),descricao);}
        else jdbc.update("UPDATE crm_campanha SET versao=?,nome=?,mes=?,cor=?,descricao=? WHERE empresa_id=? AND id=?",v,nome,f.mes(),f.cor(),descricao,empresa,id);
        jdbc.update("DELETE FROM crm_campanha_tag WHERE empresa_id=? AND campanha_id=?",empresa,id);
        for(var tag:tags) jdbc.update("INSERT INTO crm_campanha_tag(empresa_id,campanha_id,etiqueta_id) VALUES (?,?,?)",empresa,id,tag);
    }
    @Transactional public void excluirCampanha(UUID usuario,UUID id,int versao) {var empresa=acesso(usuario,true).empresaId();repo.bloquear(empresa);if(campanhaVersao(empresa,id)!=versao) throw new ConflitoException("Campanha alterada em outra sessão.");jdbc.update("DELETE FROM crm_campanha WHERE empresa_id=? AND id=?",empresa,id);}
    private List<Destinatario> destinatarios(UUID empresa,UUID campanha,int ano,boolean permiteContatos) {
        var abordados=jdbc.queryForList("SELECT destinatario FROM crm_abordagem WHERE empresa_id=? AND campanha_id=? AND ano=?",String.class,empresa,campanha,ano);
        var ids=jdbc.queryForList("SELECT DISTINCT l.id FROM crm_lead l JOIN crm_etiqueta t ON t.lead_id=l.id JOIN crm_campanha_tag c ON c.etiqueta_id=t.etiqueta_id WHERE l.empresa_id=? AND l.status<>'perdido' AND c.campanha_id=?",UUID.class,empresa,campanha);
        var lista=new ArrayList<Destinatario>();
        for(int i=0;i<ids.size();i+=100) for(var l:repo.carregar(empresa,ids.subList(i,Math.min(i+100,ids.size())))) {var c=l.dados().cadastro();lista.add(new Destinatario("lead:"+l.id(),"Lead",c.nome(),c.telefone().isBlank()?c.email():c.telefone(),abordados.contains("lead:"+l.id())));}
        if(permiteContatos) {
            var clientes=jdbc.queryForList("SELECT DISTINCT c.id FROM contato c JOIN contato_tag t ON t.contato_id=c.id JOIN contato_indicador i ON i.id=t.indicador_id JOIN crm_campanha_tag ct ON ct.etiqueta_id=i.etiqueta_id WHERE c.empresa_id=? AND c.tipo='Cliente' AND ct.campanha_id=?",UUID.class,empresa,campanha);
            for(int i=0;i<clientes.size();i+=100) for(var c:contatos.carregarPagina(empresa,clientes.subList(i,Math.min(i+100,clientes.size())))) lista.add(new Destinatario("contato:"+c.id(),"Cliente",c.dados().get("nome"),c.dados().get("telefone").isEmpty()?c.dados().get("email"):c.dados().get("telefone"),abordados.contains("contato:"+c.id())));
        }
        return lista.stream().sorted(Comparator.comparing(Destinatario::nome,String.CASE_INSENSITIVE_ORDER)).toList();
    }
    public Marketing marketing(UUID usuario,int ano) {
        var a=acesso(usuario,false);if(ano<2000 || ano>2200) throw new IllegalArgumentException("Ano inválido.");
        var campanhas=jdbc.query("SELECT * FROM crm_campanha WHERE empresa_id=? ORDER BY mes NULLS LAST,nome,id",(r,n)->new Campanha(r.getObject("id",UUID.class),r.getInt("versao"),r.getString("nome"),(Integer)r.getObject("mes"),r.getString("cor"),r.getString("descricao"),List.of(),List.of()),a.empresaId());
        var itens=campanhas.stream().map(c->new Campanha(c.id(),c.versao(),c.nome(),c.mes(),c.cor(),c.descricao(),campanhaTags(a.empresaId(),c.id()),destinatarios(a.empresaId(),c.id(),ano,a.modulos().contains("contatos")))).toList();
        var manutencoes=a.modulos().contains("casos")?jdbc.queryForList("SELECT m.caso_id,m.data_entrega,m.meses,m.ultimo,m.momento,c.titulo FROM crm_manutencao m JOIN caso c ON c.id=m.caso_id WHERE m.empresa_id=? ORDER BY c.titulo",a.empresaId()):List.<Map<String,Object>>of();
        return new Marketing(itens,manutencoes,ano);
    }
    @Transactional public void abordar(UUID usuario,UUID id,AbordagemRequest f) {
        var a=acesso(usuario,true);repo.bloquear(a.empresaId());campanhaVersao(a.empresaId(),id);
        if(f.ano()!=LocalDate.now(ZONA).getYear() || destinatarios(a.empresaId(),id,f.ano(),a.modulos().contains("contatos")).stream().noneMatch(d->d.chave().equals(f.destinatario()))) throw new IllegalArgumentException("Destinatário ou ano inválido para esta campanha.");
        jdbc.update("DELETE FROM crm_abordagem WHERE empresa_id=? AND campanha_id=? AND destinatario=? AND ano=?",a.empresaId(),id,f.destinatario(),f.ano());
        if(f.abordado()) jdbc.update("INSERT INTO crm_abordagem(empresa_id,campanha_id,destinatario,ano,autor_id) VALUES (?,?,?,?,?)",a.empresaId(),id,f.destinatario(),f.ano(),usuario);
    }
    @Transactional public void registrarManutencao(UUID usuario,UUID caso) {
        var a=acesso(usuario,true);if(!a.modulos().contains("casos")) throw new AccessDeniedException("Você não tem acesso a Casos.");
        if(jdbc.update("UPDATE crm_manutencao SET ultimo=?,momento='Contato enviado' WHERE empresa_id=? AND caso_id=?",LocalDate.now(ZONA),a.empresaId(),caso)==0) throw new RecursoNaoEncontradoException("Manutenção não encontrada.");
    }
    public List<Map<String,Object>> notificacoes(UUID usuario) {var a=acesso(usuario,false);return jdbc.queryForList("SELECT n.id,c.lead_id,c.texto,u.nome AS autor,n.criado_em FROM crm_notificacao n JOIN crm_comentario c ON c.id=n.comentario_id JOIN usuario u ON u.id=c.autor_id WHERE n.empresa_id=? AND n.usuario_id=? AND n.lida=false ORDER BY n.criado_em DESC LIMIT 50",a.empresaId(),usuario);}
    @Transactional public void lerNotificacao(UUID usuario,UUID id) {var a=acesso(usuario,false);if(jdbc.update("UPDATE crm_notificacao SET lida=true WHERE empresa_id=? AND usuario_id=? AND id=?",a.empresaId(),usuario,id)==0) throw new RecursoNaoEncontradoException("Notificação não encontrada.");}
}
