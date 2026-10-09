package com.advocacia_microservice.financeiro;

import com.advocacia_microservice.financeiro.application.FinanceiroConfigService;
import com.advocacia_microservice.financeiro.domain.FinanceiroConfig;
import com.advocacia_microservice.financeiro.domain.FinanceiroConfig.*;
import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.Usuario;
import com.advocacia_microservice.shared.exception.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test")
class FinanceiroConfigTests {
    @Autowired FinanceiroConfigService service;
    @Autowired EmpresaService empresas;
    @Autowired EmpresaRepository empresaRepository;
    @Autowired CriarUsuarioUseCase criarUsuario;
    @Autowired WebApplicationContext context;
    Usuario master; UUID empresa; MockMvc mvc;
    Usuario usuario() { return criarUsuario.executar("Teste",UUID.randomUUID()+"@example.test"); }
    UUID empresa(Usuario u) { return empresas.criar(u.id(),Map.of("razao_social","Teste","cnpj",UUID.randomUUID().toString().substring(0,14),"endereco","Rua A","cidade","Brasília")).id(); }
    @BeforeEach void preparar() { master=usuario();empresa=empresa(master);mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }
    BigDecimal n(String v) { return new BigDecimal(v); }
    Conta conta(String nome,boolean padrao,boolean ativo) { return new Conta(null,nome,"Banco","conta",n("-12.34"),padrao,ativo); }
    @Test void persisteContaPelaApiEMantemUmUnicoPadraoAtivo() throws Exception {
        mvc.perform(post("/api/config/financeiro/contas").with(user(master.id().toString())).with(csrf()).contentType("application/json").content("""
            {"versao":0,"dados":{"nome":" Principal ","banco":"Banco","tipo":"conta","saldo":12.34,"padrao":true,"ativo":true}}
            """)).andExpect(status().isCreated()).andExpect(jsonPath("$.versao").value(1)).andExpect(jsonPath("$.contas[0].saldo").value(12.34));
        var primeiro=service.buscar(master.id()).contas().getFirst();
        var dados=service.conta(master.id(),1,null,conta("Outra",true,true));
        assertEquals(1,dados.contas().stream().filter(Conta::padrao).count());
        assertFalse(dados.contas().stream().filter(c->c.id().equals(primeiro.id())).findFirst().orElseThrow().padrao());
        var outra=dados.contas().stream().filter(Conta::padrao).findFirst().orElseThrow();
        service.conta(master.id(),2,outra.id(),conta("Outra",false,false));
        assertTrue(service.buscar(master.id()).contas().stream().noneMatch(Conta::padrao));
        assertThrows(IllegalArgumentException.class,()->conta("Inválida",true,false));
    }
    @Test void persisteCategoriasCentrosECustosEExcluiSomenteOCustoEscolhido() throws Exception {
        service.categoria(master.id(),0,null,new Categoria(null,"Honorários",FinanceiroConfig.GRUPOS.get(1),"entrada",true));
        var centro=service.centro(master.id(),1,null,new Centro(null,"Cível",true)).centros().getFirst();
        service.centro(master.id(),2,centro.id(),new Centro(null,"Cível atualizado",false));
        var custo=service.custo(master.id(),3,new Custo(null,"Aluguel",n("1250.25"))).custos().getFirst();
        var salvo=service.buscar(master.id()); assertEquals("Cível atualizado",salvo.centros().getFirst().nome()); assertFalse(salvo.centros().getFirst().ativo());
        assertEquals(n("1250.25"),salvo.custos().getFirst().valor()); assertEquals("Honorários",salvo.categorias().getFirst().nome());
        mvc.perform(delete("/api/config/financeiro/custos/"+custo.id()).param("versao","4").with(user(master.id().toString())).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.custos.length()").value(0));
        assertEquals(1,service.buscar(master.id()).categorias().size());
    }
    @Test void urhECapacidadeSalvamSeparadamenteEConservamPrecisao() throws Exception {
        mvc.perform(put("/api/config/financeiro/urh").with(user(master.id().toString())).with(csrf()).contentType("application/json").content("{\"versao\":0,\"dados\":{\"urh\":321.45,\"competencia\":\"2026-10\"}}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.urh").value(321.45));
        var capacidade=new Capacidade(n("120"),"direto",2,n("40"),n("65"),n("4.2"),n("35"),n("3.5"));
        service.capacidade(master.id(),1,capacidade);
        var salvo=service.buscar(master.id()); assertEquals(capacidade,salvo.capacidade()); assertEquals(n("321.45"),salvo.urh()); assertEquals("2026-10",salvo.competencia());
        service.urh(master.id(),2,new Urh(n("350"),"2026-11")); assertEquals(capacidade,service.buscar(master.id()).capacidade());
    }
    @Test void versaoEDuplicidadeNaoApagamDadosNemTrocamContaPadrao() throws Exception {
        var primeiro=service.conta(master.id(),0,null,conta("Principal",true,true)).contas().getFirst();
        assertThrows(ConflitoException.class,()->service.custo(master.id(),0,new Custo(null,"Aluguel",n("100"))));
        mvc.perform(post("/api/config/financeiro/contas").with(user(master.id().toString())).with(csrf()).contentType("application/json").content("{\"versao\":1,\"dados\":{\"nome\":\" principal \",\"banco\":\"Banco\",\"tipo\":\"conta\",\"saldo\":0,\"padrao\":true,\"ativo\":true}}"))
            .andExpect(status().isConflict());
        var dados=service.buscar(master.id()); assertEquals(1,dados.versao()); assertEquals(primeiro.id(),dados.contas().getFirst().id()); assertTrue(dados.contas().getFirst().padrao()); assertTrue(dados.custos().isEmpty());
    }
    @Test void isolamentoPermissoesSessaoCsrfEVinculo() throws Exception {
        var custo=service.custo(master.id(),0,new Custo(null,"Custo",n("10"))).custos().getFirst();
        var outro=usuario(); empresa(outro); assertTrue(service.buscar(outro.id()).custos().isEmpty());
        assertThrows(RecursoNaoEncontradoException.class,()->service.excluirCusto(outro.id(),0,custo.id()));
        assertEquals(0,service.buscar(outro.id()).versao());
        var membro=usuario(); empresaRepository.vincular(new VinculoEmpresa(membro.id(),empresa,PapelEmpresa.MEMBRO));
        mvc.perform(get("/api/config/financeiro").with(user(membro.id().toString()))).andExpect(status().isOk()).andExpect(jsonPath("$.papel").value("MEMBRO"));
        mvc.perform(put("/api/config/financeiro/urh").with(user(membro.id().toString())).with(csrf()).contentType("application/json").content("{\"versao\":1,\"dados\":{\"urh\":300,\"competencia\":\"2026-10\"}}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/config/financeiro")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/config/financeiro/custos").with(user(master.id().toString())).contentType("application/json").content("{\"versao\":1,\"dados\":{\"nome\":\"Custo\",\"valor\":10}}"))
            .andExpect(status().isForbidden());
        var semEmpresa=usuario(); assertNull(service.buscar(semEmpresa.id()).empresaId());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.custo(semEmpresa.id(),0,new Custo(null,"Custo",n("10"))));
    }
    @Test void validaPrecisaoNumerosTiposCompetenciaERequisicoesIncompletas() throws Exception {
        assertThrows(IllegalArgumentException.class,()->new Custo(null,"Custo",n("0")));
        assertThrows(IllegalArgumentException.class,()->new Custo(null,"Custo",n("1.234")));
        assertThrows(IllegalArgumentException.class,()->new Urh(n("10"),"2026-13"));
        assertThrows(IllegalArgumentException.class,()->new Categoria(null,"Categoria",null,"entrada",true));
        assertThrows(IllegalArgumentException.class,()->new Conta(null,"Conta","","invalido",n("0"),false,true));
        assertThrows(IllegalArgumentException.class,()->new Capacidade(n("0"),"direto",1,n("40"),n("60"),n("4.2"),n("30"),n("3")));
        mvc.perform(put("/api/config/financeiro/urh").with(user(master.id().toString())).with(csrf()).contentType("application/json").content("{\"dados\":{\"urh\":10,\"competencia\":\"2026-10\"}}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/config/financeiro/custos").with(user(master.id().toString())).with(csrf()).contentType("application/json").content("{\"versao\":0,\"dados\":{\"nome\":\"Custo\",\"valor\":-10}}"))
            .andExpect(status().isBadRequest());
    }
}
