package com.advocacia_microservice.caso.application;

import com.advocacia_microservice.cliente.infrastructure.persistence.repository.ContatoRepository;
import com.advocacia_microservice.crm.infrastructure.CrmRepository;
import com.advocacia_microservice.equipe.application.EquipeService;
import com.advocacia_microservice.empresa.domain.PapelEmpresa;
import com.advocacia_microservice.shared.exception.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class TarefasCasoService {
    public record Dados(UUID tipoId,String titulo,LocalDate data,LocalDate dataFim,
            LocalTime hora,LocalTime horaFim,LocalDate prazoFatal,String local,String descricao,
            boolean mostrarAgenda,boolean diaInteiro,String repeticao,List<String> flags,List<UUID> responsaveis) {}
    public record Formulario(Integer versao,Dados dados) {}
    public record Atualizacao(Integer versao,String status,String providencia) {}
    public record Adiamento(Integer versao,LocalDate data) {}
    public record Delegacao(Integer versao,List<UUID> responsaveis) {}
    public record ComentarioRequest(Integer versao,String texto) {}
    public record Comentario(UUID id,String texto,String autor,OffsetDateTime criadoEm) {}
    public record Tarefa(UUID id,int versao,String status,Dados dados,String tipo,
            List<CasosService.Opcao> responsaveis,String concluidaPor,OffsetDateTime concluidaEm,
            List<Comentario> comentarios) {}
    public record Opcoes(boolean podeEditar,List<CasosService.Opcao> tipos,List<CasosService.Opcao> membros) {}
    private record Registro(UUID id,UUID autor,int versao,String status,Dados dados,String tipo,
            String concluidaPor,OffsetDateTime concluidaEm) {}
    private final JdbcTemplate jdbc;
    private final CasosService casos;
    private final EquipeService equipe;
    private final ContatoRepository lock;
    private final CrmRepository json;

    public TarefasCasoService(JdbcTemplate jdbc,CasosService casos,EquipeService equipe,ContatoRepository lock,CrmRepository json) {
        this.jdbc=jdbc;this.casos=casos;this.equipe=equipe;this.lock=lock;this.json=json;
    }
    private UUID empresa(UUID usuario,UUID caso,boolean escrita) {
        var acesso=casos.acesso(usuario,escrita);
        var empresa=acesso.empresaId();
        if(escrita) lock.bloquear(empresa);
        var status=casos.exigir(empresa,caso).status();
        if(escrita&&!status.equals("ativo")) throw new ConflitoException("Reative o caso antes de alterar suas tarefas.");
        return empresa;
    }
    public Opcoes opcoes(UUID usuario,UUID caso) {
        var empresa=empresa(usuario,caso,false);var op=casos.opcoes(usuario);
        var tipos=jdbc.query("SELECT id,nome FROM cadastro_item WHERE empresa_id=? AND tipo='tarefas' AND ativo=true ORDER BY nome",(r,n)->new CasosService.Opcao(r.getObject(1,UUID.class),r.getString(2)),empresa);
        return new Opcoes(op.podeEditar(),tipos,op.membros());
    }
    private static final String SELECT="SELECT t.*,u.nome concluida_por_nome FROM caso_tarefa t LEFT JOIN usuario u ON u.id=t.concluida_por";
    private Registro mapear(ResultSet r,int n)throws SQLException {
        var dados=json.ler(r.getString("dados"),Dados.class);
        var tipo=dados.tipoId()==null?null:jdbc.query("SELECT nome FROM cadastro_item WHERE id=?",(s,i)->s.getString(1),dados.tipoId()).stream().findFirst().orElse(null);
        return new Registro(r.getObject("id",UUID.class),r.getObject("autor_id",UUID.class),r.getInt("versao"),r.getString("status"),dados,tipo,r.getString("concluida_por_nome"),r.getObject("concluida_em",OffsetDateTime.class));
    }
    private boolean visivel(UUID usuario,Registro r) {
        return !r.dados().flags().contains("Privada")||r.autor().equals(usuario)||r.dados().responsaveis().contains(usuario)||equipe.acesso(usuario).papel()==PapelEmpresa.MASTER;
    }
    private Registro exigir(UUID usuario,UUID empresa,UUID caso,UUID id,Integer versao) {
        var r=jdbc.query(SELECT+" WHERE t.empresa_id=? AND t.caso_id=? AND t.id=?",this::mapear,empresa,caso,id).stream().filter(t->visivel(usuario,t)).findFirst().orElseThrow(()->new RecursoNaoEncontradoException("Tarefa não encontrada."));
        if(versao!=null&&versao!=r.versao()) throw new ConflitoException("Esta tarefa foi alterada em outra sessão. Recarregue antes de continuar.");
        return r;
    }
    private void versao(Integer v){if(v==null||v<0)throw new IllegalArgumentException("Informe a versão da tarefa.");}
    private Tarefa ficha(UUID empresa,Registro r) {
        var responsaveis=jdbc.query("SELECT u.id,u.nome FROM caso_tarefa_responsavel t JOIN usuario u ON u.id=t.usuario_id WHERE t.empresa_id=? AND t.tarefa_id=? ORDER BY u.nome",(s,n)->new CasosService.Opcao(s.getObject(1,UUID.class),s.getString(2)),empresa,r.id());
        var comentarios=jdbc.query("SELECT c.*,u.nome FROM caso_tarefa_comentario c JOIN usuario u ON u.id=c.autor_id WHERE c.empresa_id=? AND c.tarefa_id=? ORDER BY c.criado_em DESC,c.id",(s,n)->new Comentario(s.getObject("id",UUID.class),s.getString("texto"),s.getString("nome"),s.getObject("criado_em",OffsetDateTime.class)),empresa,r.id());
        return new Tarefa(r.id(),r.versao(),r.status(),r.dados(),r.tipo(),responsaveis,r.concluidaPor(),r.concluidaEm(),comentarios);
    }
    public List<Tarefa> listar(UUID usuario,UUID caso) {
        var empresa=empresa(usuario,caso,false);
        return jdbc.query(SELECT+" WHERE t.empresa_id=? AND t.caso_id=?",this::mapear,empresa,caso).stream().filter(r->visivel(usuario,r))
                .sorted(Comparator.comparing((Registro r)->r.dados().data(),Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Registro::id))
                .map(r->ficha(empresa,r)).toList();
    }
    public Tarefa buscar(UUID usuario,UUID caso,UUID id){var empresa=empresa(usuario,caso,false);return ficha(empresa,exigir(usuario,empresa,caso,id,null));}
    private String texto(String s,int limite){s=Objects.requireNonNullElse(s,"").strip();if(s.length()>limite)throw new IllegalArgumentException("Texto acima do limite permitido.");return s;}
    private void data(LocalDate d){if(d!=null&&(d.getYear()<1900||d.getYear()>2200))throw new IllegalArgumentException("Data inválida.");}
    private List<UUID> responsaveis(List<UUID> ids,Opcoes op){
        if(ids==null||ids.size()>50||ids.stream().anyMatch(Objects::isNull)||new HashSet<>(ids).size()!=ids.size())throw new IllegalArgumentException("Responsáveis inválidos.");
        if(ids.stream().anyMatch(id->op.membros().stream().noneMatch(m->m.id().equals(id))))throw new IllegalArgumentException("Selecione responsáveis ativos da empresa com acesso a Casos.");
        return List.copyOf(ids);
    }
    private Dados normalizar(Dados d,Opcoes op,Registro anterior){
        if(d==null)throw new IllegalArgumentException("Informe os dados da tarefa.");
        var titulo=texto(d.titulo(),300);
        if(d.tipoId()==null&&titulo.isBlank())throw new IllegalArgumentException("Escolha o tipo de tarefa ou escreva um título.");
        if(d.tipoId()!=null&&op.tipos().stream().noneMatch(t->t.id().equals(d.tipoId()))&&(anterior==null||!d.tipoId().equals(anterior.dados().tipoId())))throw new IllegalArgumentException("Selecione um tipo de tarefa ativo da empresa.");
        data(d.data());data(d.dataFim());data(d.prazoFatal());
        var inicio=d.data()==null?d.prazoFatal():d.data();
        if(d.dataFim()!=null&&(inicio==null||d.dataFim().isBefore(inicio)))throw new IllegalArgumentException("A data final não pode ser anterior à inicial.");
        if(!d.diaInteiro()&&d.horaFim()!=null&&(d.hora()==null||inicio==null||((d.dataFim()==null||d.dataFim().equals(inicio))&&!d.horaFim().isAfter(d.hora()))))throw new IllegalArgumentException("Confira o horário de término.");
        if(d.hora()!=null&&inicio==null)throw new IllegalArgumentException("Informe a data para definir um horário.");
        if(d.repeticao()==null||!Set.of("nunca","diaria","semanal","mensal","anual").contains(d.repeticao()))throw new IllegalArgumentException("Repetição inválida.");
        if(d.flags()==null||d.flags().size()>4||d.flags().stream().anyMatch(f->f==null||!Set.of("Importante","Urgente","Recorrente","Privada").contains(f))||new HashSet<>(d.flags()).size()!=d.flags().size())throw new IllegalArgumentException("Marcadores inválidos.");
        return new Dados(d.tipoId(),titulo,inicio,d.dataFim(),d.diaInteiro()?null:d.hora(),d.diaInteiro()?null:d.horaFim(),d.prazoFatal(),texto(d.local(),500),texto(d.descricao(),4000),d.mostrarAgenda(),d.diaInteiro(),d.repeticao(),List.copyOf(d.flags()),responsaveis(d.responsaveis(),op));
    }
    private void persistir(UUID empresa,UUID id,Dados dados){
        jdbc.update("UPDATE caso_tarefa SET dados=?,versao=versao+1,atualizado_em=CURRENT_TIMESTAMP WHERE empresa_id=? AND id=?",json.json(dados),empresa,id);
        jdbc.update("DELETE FROM caso_tarefa_responsavel WHERE empresa_id=? AND tarefa_id=?",empresa,id);
        for(var u:dados.responsaveis())jdbc.update("INSERT INTO caso_tarefa_responsavel(empresa_id,tarefa_id,usuario_id) VALUES (?,?,?)",empresa,id,u);
    }
    @Transactional public Tarefa salvar(UUID usuario,UUID caso,UUID id,Formulario f){
        var empresa=empresa(usuario,caso,true);Registro anterior=null;
        if(id!=null){versao(f.versao());anterior=exigir(usuario,empresa,caso,id,f.versao());}
        var dados=normalizar(f.dados(),opcoes(usuario,caso),anterior);
        if(id==null){id=UUID.randomUUID();jdbc.update("INSERT INTO caso_tarefa(id,empresa_id,caso_id,dados,autor_id) VALUES (?,?,?,?,?)",id,empresa,caso,json.json(dados),usuario);for(var u:dados.responsaveis())jdbc.update("INSERT INTO caso_tarefa_responsavel(empresa_id,tarefa_id,usuario_id) VALUES (?,?,?)",empresa,id,u);}
        else persistir(empresa,id,dados);
        return ficha(empresa,exigirInterno(empresa,caso,id));
    }
    private Registro exigirInterno(UUID empresa,UUID caso,UUID id){return jdbc.query(SELECT+" WHERE t.empresa_id=? AND t.caso_id=? AND t.id=?",this::mapear,empresa,caso,id).getFirst();}
    private void comentar(UUID empresa,UUID id,UUID usuario,String texto){jdbc.update("INSERT INTO caso_tarefa_comentario(id,empresa_id,tarefa_id,autor_id,texto) VALUES (?,?,?,?,?)",UUID.randomUUID(),empresa,id,usuario,texto);}
    @Transactional public Tarefa estado(UUID usuario,UUID caso,UUID id,Atualizacao f){
        var empresa=empresa(usuario,caso,true);versao(f.versao());var t=exigir(usuario,empresa,caso,id,f.versao());
        if(f.status()==null||!Set.of("pendente","concluida").contains(f.status())||f.status().equals(t.status()))throw new IllegalArgumentException("Situação inválida.");
        var providencia=texto(f.providencia(),4000);boolean concluida=f.status().equals("concluida");
        jdbc.update("UPDATE caso_tarefa SET status=?,concluida_por=?,concluida_em=?,versao=versao+1,atualizado_em=CURRENT_TIMESTAMP WHERE empresa_id=? AND id=?",f.status(),concluida?usuario:null,concluida?OffsetDateTime.now(ZoneOffset.UTC):null,empresa,id);
        if(concluida&&!providencia.isBlank())comentar(empresa,id,usuario,providencia);
        return ficha(empresa,exigirInterno(empresa,caso,id));
    }
    @Transactional public Tarefa adiar(UUID usuario,UUID caso,UUID id,Adiamento f){
        var empresa=empresa(usuario,caso,true);versao(f.versao());var t=exigir(usuario,empresa,caso,id,f.versao());
        data(f.data());if(f.data()==null)throw new IllegalArgumentException("Informe a nova data.");
        var d=t.dados();long dias=d.data()==null?0:java.time.temporal.ChronoUnit.DAYS.between(d.data(),f.data());
        var fim=d.dataFim()==null?null:d.dataFim().plusDays(dias);data(fim);
        persistir(empresa,id,new Dados(d.tipoId(),d.titulo(),f.data(),fim,d.hora(),d.horaFim(),d.prazoFatal()==null?null:f.data(),d.local(),d.descricao(),d.mostrarAgenda(),d.diaInteiro(),d.repeticao(),d.flags(),d.responsaveis()));
        return ficha(empresa,exigirInterno(empresa,caso,id));
    }
    @Transactional public Tarefa delegar(UUID usuario,UUID caso,UUID id,Delegacao f){
        var empresa=empresa(usuario,caso,true);versao(f.versao());var t=exigir(usuario,empresa,caso,id,f.versao());var ids=responsaveis(f.responsaveis(),opcoes(usuario,caso));var d=t.dados();
        persistir(empresa,id,new Dados(d.tipoId(),d.titulo(),d.data(),d.dataFim(),d.hora(),d.horaFim(),d.prazoFatal(),d.local(),d.descricao(),d.mostrarAgenda(),d.diaInteiro(),d.repeticao(),d.flags(),ids));
        return ficha(empresa,exigirInterno(empresa,caso,id));
    }
    @Transactional public Tarefa comentar(UUID usuario,UUID caso,UUID id,ComentarioRequest f){
        var empresa=empresa(usuario,caso,true);versao(f.versao());exigir(usuario,empresa,caso,id,f.versao());var texto=texto(f.texto(),4000);if(texto.isBlank())throw new IllegalArgumentException("Escreva um comentário.");
        comentar(empresa,id,usuario,texto);jdbc.update("UPDATE caso_tarefa SET versao=versao+1,atualizado_em=CURRENT_TIMESTAMP WHERE empresa_id=? AND id=?",empresa,id);return ficha(empresa,exigirInterno(empresa,caso,id));
    }
    @Transactional public void excluir(UUID usuario,UUID caso,UUID id,int versao){var empresa=empresa(usuario,caso,true);exigir(usuario,empresa,caso,id,versao);jdbc.update("DELETE FROM caso_tarefa WHERE empresa_id=? AND caso_id=? AND id=?",empresa,caso,id);}
}
