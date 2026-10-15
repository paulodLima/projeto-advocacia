package com.advocacia_microservice.caso.application;

import com.advocacia_microservice.cliente.infrastructure.persistence.ContatoRepository;
import com.advocacia_microservice.equipe.application.EquipeService;
import com.advocacia_microservice.shared.exception.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class FinanceiroCasoService {
    public record Categoria(UUID id,String nome,String direcao) {}
    public record Opcoes(boolean podeEditar,List<Categoria> categorias,List<CasosService.Opcao> contatos) {}
    public record Honorario(UUID id,String tipo,BigDecimal valor,BigDecimal percentual,BigDecimal baseCalculo,String observacoes) {}
    public record HonorarioRequest(Long versao,String tipo,BigDecimal valor,BigDecimal percentual,BigDecimal baseCalculo,String observacoes) {}
    public record Lancamento(UUID id,UUID grupo,String descricao,BigDecimal valor,String direcao,String modalidade,LocalDate vencimento,int parcela,int parcelas,UUID categoriaId,String categoria,UUID contatoId,String contato,String observacoes,boolean pago,LocalDate pagoEm,String titularidade) {}
    public record LancamentoRequest(Long versao,String descricao,BigDecimal valor,String direcao,String modalidade,LocalDate vencimento,int parcelas,UUID categoriaId,UUID contatoId,String observacoes) {}
    public record Baixa(Long versao,boolean pago,LocalDate data) {}
    public record Vencimento(Long versao,LocalDate data) {}
    public record Titularidade(Long versao,String titularidade) {}
    public record Despesa(UUID id,String descricao,BigDecimal valor,LocalDate data,boolean reembolsavel,boolean reembolsada) {}
    public record DespesaRequest(Long versao,String descricao,BigDecimal valor,LocalDate data,boolean reembolsavel) {}
    public record Reembolso(Long versao,boolean reembolsada) {}
    public record Resumo(BigDecimal honorarios,BigDecimal recebido,BigDecimal aReceber,BigDecimal despesasAReembolsar) {}
    public record Ficha(long versao,List<Honorario> honorarios,List<Lancamento> lancamentos,List<Despesa> despesas,Resumo resumo) {}
    private final JdbcTemplate jdbc;private final CasosService casos;private final EquipeService equipe;private final ContatoRepository lock;
    public FinanceiroCasoService(JdbcTemplate jdbc,CasosService casos,EquipeService equipe,ContatoRepository lock){this.jdbc=jdbc;this.casos=casos;this.equipe=equipe;this.lock=lock;}
    private UUID acesso(UUID usuario,UUID caso,boolean escrita){
        var a=casos.acesso(usuario,false);
        if(!a.modulos().contains("financeiro"))throw new AccessDeniedException("Seu perfil não tem acesso ao Financeiro.");
        if(escrita)lock.bloquear(a.empresaId());
        if(escrita&&!casos.exigir(a.empresaId(),caso).status().equals("ativo"))throw new ConflitoException("Reative o caso antes de alterar seu financeiro.");
        if(!escrita)casos.exigir(a.empresaId(),caso);
        return a.empresaId();
    }
    private String texto(String t,int tamanho,boolean obrigatorio){t=Objects.requireNonNullElse(t,"").strip();if(t.length()>tamanho||(obrigatorio&&t.isBlank()))throw new IllegalArgumentException("Confira os campos obrigatórios e os limites de texto.");return t;}
    private BigDecimal moeda(BigDecimal v,boolean obrigatorio){if(v==null){if(obrigatorio)throw new IllegalArgumentException("Informe um valor positivo.");return null;}if(v.signum()<=0||v.scale()>2||v.compareTo(new BigDecimal("999999999999.99"))>0)throw new IllegalArgumentException("Informe valor positivo com até duas casas decimais.");return v.setScale(2);}
    private void data(LocalDate d){if(d==null||d.getYear()<1900||d.getYear()>2200)throw new IllegalArgumentException("Informe uma data válida.");}
    private long versao(UUID empresa,UUID caso){return jdbc.query("SELECT versao FROM caso_financeiro WHERE empresa_id=? AND caso_id=?",(r,n)->r.getLong(1),empresa,caso).stream().findFirst().orElse(0L);}
    private UUID editar(UUID usuario,UUID caso,Long v){var empresa=acesso(usuario,caso,true);if(v==null||v!=versao(empresa,caso))throw new ConflitoException("O financeiro deste caso foi alterado em outra sessão. Recarregue antes de continuar.");if(jdbc.queryForObject("SELECT COUNT(*) FROM caso_financeiro WHERE empresa_id=? AND caso_id=?",Integer.class,empresa,caso)==0)jdbc.update("INSERT INTO caso_financeiro(empresa_id,caso_id) VALUES (?,?)",empresa,caso);return empresa;}
    private void evento(UUID empresa,UUID caso,UUID usuario,String op,UUID ref){jdbc.update("UPDATE caso_financeiro SET versao=versao+1 WHERE empresa_id=? AND caso_id=?",empresa,caso);jdbc.update("INSERT INTO caso_financeiro_evento(id,empresa_id,caso_id,autor_id,operacao,referencia) VALUES (?,?,?,?,?,?)",UUID.randomUUID(),empresa,caso,usuario,op,ref);}
    private void exigir(UUID empresa,UUID caso,UUID id,String tabela){if(jdbc.queryForObject("SELECT COUNT(*) FROM "+tabela+" WHERE empresa_id=? AND caso_id=? AND id=?",Integer.class,empresa,caso,id)==0)throw new RecursoNaoEncontradoException("Lançamento não encontrado.");}
    public Opcoes opcoes(UUID usuario,UUID caso){var empresa=acesso(usuario,caso,false);var categorias=jdbc.query("SELECT id,nome,direcao FROM financeiro_categoria WHERE empresa_id=? AND ativo=true ORDER BY nome",(r,n)->new Categoria(r.getObject(1,UUID.class),r.getString(2),r.getString(3)),empresa);var contatos=jdbc.query("SELECT id,nome FROM contato WHERE empresa_id=? ORDER BY nome",(r,n)->new CasosService.Opcao(r.getObject(1,UUID.class),r.getString(2)),empresa);return new Opcoes(casos.exigir(empresa,caso).status().equals("ativo"),categorias,contatos);}
    public Ficha buscar(UUID usuario,UUID caso){
        var empresa=acesso(usuario,caso,false);
        var h=jdbc.query("SELECT * FROM caso_honorario WHERE empresa_id=? AND caso_id=? ORDER BY criado_em,id",(r,n)->new Honorario(r.getObject("id",UUID.class),r.getString("tipo"),r.getBigDecimal("valor"),r.getBigDecimal("percentual"),r.getBigDecimal("base_calculo"),r.getString("observacoes")),empresa,caso);
        var l=jdbc.query("SELECT l.*,c.nome categoria,t.nome contato FROM caso_lancamento l LEFT JOIN financeiro_categoria c ON c.id=l.categoria_id LEFT JOIN contato t ON t.id=l.contato_id WHERE l.empresa_id=? AND l.caso_id=? ORDER BY l.vencimento,l.grupo,l.parcela",(r,n)->new Lancamento(r.getObject("id",UUID.class),r.getObject("grupo",UUID.class),r.getString("descricao"),r.getBigDecimal("valor"),r.getString("direcao"),r.getString("modalidade"),r.getObject("vencimento",LocalDate.class),r.getInt("parcela"),r.getInt("parcelas"),r.getObject("categoria_id",UUID.class),r.getString("categoria"),r.getObject("contato_id",UUID.class),r.getString("contato"),r.getString("observacoes"),r.getBoolean("pago"),r.getObject("pago_em",LocalDate.class),r.getString("titularidade")),empresa,caso);
        var d=jdbc.query("SELECT * FROM caso_despesa WHERE empresa_id=? AND caso_id=? ORDER BY data DESC,id",(r,n)->new Despesa(r.getObject("id",UUID.class),r.getString("descricao"),r.getBigDecimal("valor"),r.getObject("data",LocalDate.class),r.getBoolean("reembolsavel"),r.getBoolean("reembolsada")),empresa,caso);
        var resumo=new Resumo(h.stream().map(Honorario::valor).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add),l.stream().filter(p->p.direcao().equals("entrada")&&p.pago()).map(Lancamento::valor).reduce(BigDecimal.ZERO,BigDecimal::add),l.stream().filter(p->p.direcao().equals("entrada")&&!p.pago()).map(Lancamento::valor).reduce(BigDecimal.ZERO,BigDecimal::add),d.stream().filter(p->p.reembolsavel()&&!p.reembolsada()).map(Despesa::valor).reduce(BigDecimal.ZERO,BigDecimal::add));
        return new Ficha(versao(empresa,caso),h,l,d,resumo);
    }
    @Transactional public Ficha honorario(UUID usuario,UUID caso,HonorarioRequest f){
        var empresa=editar(usuario,caso,f.versao());if(f.tipo()==null||!Set.of("contratual","exito","sucumbencia").contains(f.tipo()))throw new IllegalArgumentException("Tipo de honorário inválido.");
        var percentual=f.percentual();if(percentual!=null&&(percentual.signum()<=0||percentual.compareTo(new BigDecimal("100"))>0||percentual.scale()>2))throw new IllegalArgumentException("Percentual deve ser maior que zero e até 100, com até duas casas decimais.");
        var base=moeda(f.baseCalculo(),false);var valor=moeda(f.valor(),false);if(base!=null&&percentual==null)throw new IllegalArgumentException("Informe o percentual sobre a base de cálculo.");if(base!=null&&percentual!=null)valor=moeda(base.multiply(percentual).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP),true);if(valor==null&&percentual==null)throw new IllegalArgumentException("Informe um valor ou percentual de honorários.");
        var id=UUID.randomUUID();jdbc.update("INSERT INTO caso_honorario(id,empresa_id,caso_id,tipo,valor,percentual,base_calculo,observacoes) VALUES (?,?,?,?,?,?,?,?)",id,empresa,caso,f.tipo(),valor,percentual,base,texto(f.observacoes(),2000,false));evento(empresa,caso,usuario,"CRIAR_HONORARIO",id);return buscar(usuario,caso);
    }
    @Transactional public Ficha excluirHonorario(UUID usuario,UUID caso,UUID id,long v){var empresa=editar(usuario,caso,v);exigir(empresa,caso,id,"caso_honorario");jdbc.update("DELETE FROM caso_honorario WHERE empresa_id=? AND caso_id=? AND id=?",empresa,caso,id);evento(empresa,caso,usuario,"EXCLUIR_HONORARIO",id);return buscar(usuario,caso);}
    @Transactional public Ficha lancamento(UUID usuario,UUID caso,LancamentoRequest f){
        var empresa=editar(usuario,caso,f.versao());var valor=moeda(f.valor(),true);data(f.vencimento());if(f.direcao()==null||!Set.of("entrada","saida").contains(f.direcao())||f.modalidade()==null||!Set.of("pix","boleto","cartao","recorrente","transferencia","dinheiro").contains(f.modalidade())||f.parcelas()<1||f.parcelas()>60)throw new IllegalArgumentException("Confira direção, modalidade e quantidade de parcelas.");
        if(f.categoriaId()!=null&&jdbc.queryForObject("SELECT COUNT(*) FROM financeiro_categoria WHERE empresa_id=? AND id=? AND ativo=true AND direcao IN ('ambas',?)",Integer.class,empresa,f.categoriaId(),f.direcao())==0)throw new IllegalArgumentException("Selecione uma categoria ativa compatível da empresa.");
        if(f.contatoId()!=null&&jdbc.queryForObject("SELECT COUNT(*) FROM contato WHERE empresa_id=? AND id=?",Integer.class,empresa,f.contatoId())==0)throw new IllegalArgumentException("Selecione um contato da empresa.");
        var descricao=texto(f.descricao(),280,true);var obs=texto(f.observacoes(),2000,false);long centavos=valor.movePointRight(2).longValueExact();long parcela=centavos/f.parcelas(),resto=centavos%f.parcelas();if(parcela<1)throw new IllegalArgumentException("O total deve permitir ao menos um centavo por parcela.");var grupo=UUID.randomUUID();
        for(int i=0;i<f.parcelas();i++){var vencimento=f.vencimento().plusMonths(i);data(vencimento);var quantia=BigDecimal.valueOf(parcela+(i==0?resto:0),2);jdbc.update("INSERT INTO caso_lancamento(id,empresa_id,caso_id,grupo,descricao,valor,direcao,modalidade,vencimento,parcela,parcelas,categoria_id,contato_id,observacoes) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),empresa,caso,grupo,descricao,quantia,f.direcao(),f.modalidade(),vencimento,i+1,f.parcelas(),f.categoriaId(),f.contatoId(),obs);}
        evento(empresa,caso,usuario,"CRIAR_LANCAMENTO",grupo);return buscar(usuario,caso);
    }
    @Transactional public Ficha baixa(UUID usuario,UUID caso,UUID id,Baixa f){var empresa=editar(usuario,caso,f.versao());exigir(empresa,caso,id,"caso_lancamento");var data=f.pago()?(f.data()==null?LocalDate.now(ZoneId.of("America/Sao_Paulo")):f.data()):null;if(data!=null)data(data);jdbc.update("UPDATE caso_lancamento SET pago=?,pago_em=? WHERE empresa_id=? AND caso_id=? AND id=?",f.pago(),data,empresa,caso,id);evento(empresa,caso,usuario,f.pago()?"BAIXAR_PARCELA":"ESTORNAR_PARCELA",id);return buscar(usuario,caso);}
    @Transactional public Ficha vencimento(UUID usuario,UUID caso,UUID id,Vencimento f){var empresa=editar(usuario,caso,f.versao());exigir(empresa,caso,id,"caso_lancamento");data(f.data());if(jdbc.queryForObject("SELECT pago FROM caso_lancamento WHERE empresa_id=? AND caso_id=? AND id=?",Boolean.class,empresa,caso,id))throw new ConflitoException("Estorne a baixa antes de mudar o vencimento.");jdbc.update("UPDATE caso_lancamento SET vencimento=? WHERE empresa_id=? AND caso_id=? AND id=?",f.data(),empresa,caso,id);evento(empresa,caso,usuario,"ALTERAR_VENCIMENTO",id);return buscar(usuario,caso);}
    @Transactional public Ficha titularidade(UUID usuario,UUID caso,UUID id,Titularidade f){var empresa=editar(usuario,caso,f.versao());exigir(empresa,caso,id,"caso_lancamento");if(f.titularidade()==null||!Set.of("cliente","escritorio").contains(f.titularidade()))throw new IllegalArgumentException("Titularidade inválida.");jdbc.update("UPDATE caso_lancamento SET titularidade=? WHERE empresa_id=? AND caso_id=? AND id=?",f.titularidade(),empresa,caso,id);evento(empresa,caso,usuario,"ALTERAR_TITULARIDADE",id);return buscar(usuario,caso);}
    @Transactional public Ficha excluirLancamento(UUID usuario,UUID caso,UUID id,long v){var empresa=editar(usuario,caso,v);exigir(empresa,caso,id,"caso_lancamento");if(jdbc.queryForObject("SELECT pago FROM caso_lancamento WHERE empresa_id=? AND caso_id=? AND id=?",Boolean.class,empresa,caso,id))throw new ConflitoException("Estorne a baixa antes de excluir a parcela.");jdbc.update("DELETE FROM caso_lancamento WHERE empresa_id=? AND caso_id=? AND id=?",empresa,caso,id);evento(empresa,caso,usuario,"EXCLUIR_PARCELA",id);return buscar(usuario,caso);}
    @Transactional public Ficha despesa(UUID usuario,UUID caso,DespesaRequest f){var empresa=editar(usuario,caso,f.versao());var valor=moeda(f.valor(),true);data(f.data());var id=UUID.randomUUID();jdbc.update("INSERT INTO caso_despesa(id,empresa_id,caso_id,descricao,valor,data,reembolsavel) VALUES (?,?,?,?,?,?,?)",id,empresa,caso,texto(f.descricao(),300,true),valor,f.data(),f.reembolsavel());evento(empresa,caso,usuario,"CRIAR_DESPESA",id);return buscar(usuario,caso);}
    @Transactional public Ficha reembolso(UUID usuario,UUID caso,UUID id,Reembolso f){var empresa=editar(usuario,caso,f.versao());exigir(empresa,caso,id,"caso_despesa");if(!jdbc.queryForObject("SELECT reembolsavel FROM caso_despesa WHERE empresa_id=? AND caso_id=? AND id=?",Boolean.class,empresa,caso,id))throw new IllegalArgumentException("Esta despesa não é reembolsável.");jdbc.update("UPDATE caso_despesa SET reembolsada=? WHERE empresa_id=? AND caso_id=? AND id=?",f.reembolsada(),empresa,caso,id);evento(empresa,caso,usuario,f.reembolsada()?"REEMBOLSAR_DESPESA":"ESTORNAR_REEMBOLSO",id);return buscar(usuario,caso);}
    @Transactional public Ficha excluirDespesa(UUID usuario,UUID caso,UUID id,long v){var empresa=editar(usuario,caso,v);exigir(empresa,caso,id,"caso_despesa");if(jdbc.queryForObject("SELECT reembolsada FROM caso_despesa WHERE empresa_id=? AND caso_id=? AND id=?",Boolean.class,empresa,caso,id))throw new ConflitoException("Estorne o reembolso antes de excluir a despesa.");jdbc.update("DELETE FROM caso_despesa WHERE empresa_id=? AND caso_id=? AND id=?",empresa,caso,id);evento(empresa,caso,usuario,"EXCLUIR_DESPESA",id);return buscar(usuario,caso);}
}
