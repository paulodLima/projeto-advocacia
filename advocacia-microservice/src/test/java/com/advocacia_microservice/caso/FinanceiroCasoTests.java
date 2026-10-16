package com.advocacia_microservice.caso;
import com.advocacia_microservice.caso.application.*;
import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.Usuario;
import com.advocacia_microservice.shared.exception.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test")
class FinanceiroCasoTests {
    @Autowired FinanceiroCasoService fin;@Autowired CasosService casos;@Autowired EmpresaService empresas;
    @Autowired CriarUsuarioUseCase usuarios;@Autowired EmpresaRepository vinculos;@Autowired JdbcTemplate jdbc;@Autowired WebApplicationContext context;
    Usuario master;UUID empresa,caso;MockMvc mvc;
    Usuario usuario(){return usuarios.executar("Teste Financeiro",UUID.randomUUID()+"@example.test");}
    @Autowired com.advocacia_microservice.plataforma.domain.repository.ConviteInicialRepository convitesIniciais;
    @Autowired com.advocacia_microservice.usuario.domain.UsuarioRepository usuariosDominio;
    UUID empresa(Usuario u){return com.advocacia_microservice.empresa.EmpresaTestSupport.criar(empresas,convitesIniciais,usuariosDominio,u.id(),Map.of("razao_social","Financeiro","cnpj",UUID.randomUUID().toString().substring(0,14),"endereco","Rua A","cidade","Brasília")).id();}
    UUID caso(Usuario u){return casos.salvar(u.id(),null,new CasosService.Formulario(null,"Caso",null,null,null,"","",null,null,null)).caso().id();}
    @BeforeEach void preparar(){master=usuario();empresa=empresa(master);caso=caso(master);mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
    FinanceiroCasoService.LancamentoRequest lancamento(long versao,String direcao,String valor,int parcelas){return new FinanceiroCasoService.LancamentoRequest(versao,"Honorários",new BigDecimal(valor),direcao,"pix",LocalDate.of(2027,1,31),parcelas,null,null,"");}
    @Test void honorariosPercentuaisUsamDecimalEPermitemExpectativaSemBase(){
        var f=fin.honorario(master.id(),caso,new FinanceiroCasoService.HonorarioRequest(0L,"contratual",null,new BigDecimal("12.50"),new BigDecimal("1234.56"),"Contrato"));assertEquals(new BigDecimal("154.32"),f.honorarios().getFirst().valor());
        f=fin.honorario(master.id(),caso,new FinanceiroCasoService.HonorarioRequest(f.versao(),"exito",null,new BigDecimal("20"),null,"Expectativa"));assertEquals(2,f.honorarios().size());assertEquals(new BigDecimal("154.32"),f.resumo().honorarios());assertEquals(BigDecimal.ZERO,f.resumo().recebido());
        var v=f.versao();assertThrows(IllegalArgumentException.class,()->fin.honorario(master.id(),caso,new FinanceiroCasoService.HonorarioRequest(v,"exito",null,new BigDecimal("101"),null,"")));
    }
    @Test void parcelamentoPreservaCentavosEDiaDoVencimento(){
        var f=fin.lancamento(master.id(),caso,lancamento(0,"entrada","100.00",3));var p=f.lancamentos();assertEquals(3,p.size());assertEquals(new BigDecimal("33.34"),p.get(0).valor());assertEquals(new BigDecimal("33.33"),p.get(1).valor());assertEquals(LocalDate.of(2027,2,28),p.get(1).vencimento());assertEquals(LocalDate.of(2027,3,31),p.get(2).vencimento());assertEquals(new BigDecimal("100.00"),f.resumo().aReceber());assertEquals(1,p.stream().map(FinanceiroCasoService.Lancamento::grupo).distinct().count());
        assertThrows(ConflitoException.class,()->fin.lancamento(master.id(),caso,lancamento(0,"entrada","100",3)));assertEquals(3,fin.buscar(master.id(),caso).lancamentos().size());
    }
    @Test void baixaEstornoESaidaNaoInflamRecebimentos(){
        var f=fin.lancamento(master.id(),caso,lancamento(0,"entrada","100",1));var entrada=f.lancamentos().getFirst().id();f=fin.baixa(master.id(),caso,entrada,new FinanceiroCasoService.Baixa(f.versao(),true,LocalDate.of(2027,1,20)));assertEquals(new BigDecimal("100.00"),f.resumo().recebido());assertEquals(BigDecimal.ZERO,f.resumo().aReceber());
        f=fin.lancamento(master.id(),caso,lancamento(f.versao(),"saida","40",1));var saida=f.lancamentos().stream().filter(p->p.direcao().equals("saida")).findFirst().orElseThrow().id();f=fin.baixa(master.id(),caso,saida,new FinanceiroCasoService.Baixa(f.versao(),true,null));assertEquals(new BigDecimal("100.00"),f.resumo().recebido());
        var v=f.versao();assertThrows(ConflitoException.class,()->fin.excluirLancamento(master.id(),caso,entrada,v));assertThrows(ConflitoException.class,()->fin.vencimento(master.id(),caso,entrada,new FinanceiroCasoService.Vencimento(v,LocalDate.now())));
        f=fin.baixa(master.id(),caso,entrada,new FinanceiroCasoService.Baixa(v,false,null));assertNull(f.lancamentos().stream().filter(p->p.id().equals(entrada)).findFirst().orElseThrow().pagoEm());assertEquals(new BigDecimal("100.00"),f.resumo().aReceber());
    }
    @Test void despesasReembolsoETitularidadePersistemEHistoricoImpedeExcluirCaso(){
        var f=fin.despesa(master.id(),caso,new FinanceiroCasoService.DespesaRequest(0L,"Cartório",new BigDecimal("30.00"),LocalDate.now(),true));assertEquals(new BigDecimal("30.00"),f.resumo().despesasAReembolsar());var d=f.despesas().getFirst().id();
        f=fin.reembolso(master.id(),caso,d,new FinanceiroCasoService.Reembolso(f.versao(),true));assertEquals(BigDecimal.ZERO,f.resumo().despesasAReembolsar());var v=f.versao();assertThrows(ConflitoException.class,()->fin.excluirDespesa(master.id(),caso,d,v));
        f=fin.lancamento(master.id(),caso,lancamento(v,"entrada","50",1));var id=f.lancamentos().getFirst().id();f=fin.titularidade(master.id(),caso,id,new FinanceiroCasoService.Titularidade(f.versao(),"cliente"));assertEquals("cliente",f.lancamentos().getFirst().titularidade());
        assertThrows(ConflitoException.class,()->casos.excluir(master.id(),caso,0));assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM caso_financeiro_evento WHERE empresa_id=? AND caso_id=?",Integer.class,empresa,caso)>0);
    }
    @Test void valoresInvalidosOuCategoriaExternaFazemRollback(){
        assertThrows(IllegalArgumentException.class,()->fin.lancamento(master.id(),caso,lancamento(0,"entrada","0.02",3)));assertEquals(0,fin.buscar(master.id(),caso).versao());
        var fora=usuario();var outra=empresa(fora);var cat=UUID.randomUUID();jdbc.update("INSERT INTO financeiro_categoria(id,empresa_id,nome,nome_chave,grupo,direcao,ativo) VALUES (?,?,'Categoria','categoria','','entrada',true)",cat,outra);
        assertThrows(IllegalArgumentException.class,()->fin.lancamento(master.id(),caso,new FinanceiroCasoService.LancamentoRequest(0L,"Erro",BigDecimal.TEN,"entrada","pix",LocalDate.now(),1,cat,null,"")));assertTrue(fin.buscar(master.id(),caso).lancamentos().isEmpty());
        assertThrows(IllegalArgumentException.class,()->fin.despesa(master.id(),caso,new FinanceiroCasoService.DespesaRequest(0L,"Erro",new BigDecimal("-1"),LocalDate.now(),true)));
    }
    @Test void acessoFinanceiroExigeOsDoisModulosEIsolaEmpresaECaso(){
        var outro=usuario();empresa(outro);var outroCaso=caso(outro);var f=fin.lancamento(master.id(),caso,lancamento(0,"entrada","10",1));var id=f.lancamentos().getFirst().id();
        assertThrows(RecursoNaoEncontradoException.class,()->fin.buscar(outro.id(),caso));assertThrows(RecursoNaoEncontradoException.class,()->fin.baixa(outro.id(),outroCaso,id,new FinanceiroCasoService.Baixa(0L,true,null)));
        var membro=usuario();vinculos.vincular(new VinculoEmpresa(membro.id(),empresa,PapelEmpresa.MEMBRO));jdbc.update("INSERT INTO equipe_membro(usuario_id,perfil,restrito) VALUES (?,'FINANCEIRO',true)",membro.id());jdbc.update("INSERT INTO equipe_membro_aba(usuario_id,aba) VALUES (?,'casos')",membro.id());assertThrows(AccessDeniedException.class,()->fin.buscar(membro.id(),caso));assertFalse(casos.opcoes(membro.id()).podeVerFinanceiro());
        jdbc.update("INSERT INTO equipe_membro_aba(usuario_id,aba) VALUES (?,'financeiro')",membro.id());assertTrue(casos.opcoes(membro.id()).podeVerFinanceiro());assertEquals(1,fin.buscar(membro.id(),caso).lancamentos().size());fin.baixa(membro.id(),caso,id,new FinanceiroCasoService.Baixa(f.versao(),true,null));
        casos.situacao(master.id(),caso,new CasosService.SituacaoRequest(0,"encerrado"));assertFalse(fin.opcoes(master.id(),caso).podeEditar());assertThrows(ConflitoException.class,()->fin.despesa(master.id(),caso,new FinanceiroCasoService.DespesaRequest(2L,"Erro",BigDecimal.TEN,LocalDate.now(),true)));
    }
    @Test void endpointsExigemCsrfEMantemCachePrivado()throws Exception{
        var url="/api/casos/"+caso+"/financeiro";mvc.perform(get(url)).andExpect(status().isUnauthorized());mvc.perform(get(url).with(user(master.id().toString()))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        var body="{\"versao\":0,\"tipo\":\"contratual\",\"valor\":100}";mvc.perform(post(url+"/honorarios").with(user(master.id().toString())).contentType("application/json").content(body)).andExpect(status().isForbidden());mvc.perform(post(url+"/honorarios").with(user(master.id().toString())).with(csrf()).contentType("application/json").content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.versao").value(1));
    }
}
