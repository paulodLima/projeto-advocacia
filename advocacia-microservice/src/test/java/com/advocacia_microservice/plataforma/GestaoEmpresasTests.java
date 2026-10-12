package com.advocacia_microservice.plataforma;

import com.advocacia_microservice.empresa.application.EmpresaService;
import com.advocacia_microservice.empresa.EmpresaTestSupport;
import com.advocacia_microservice.plataforma.application.GestaoEmpresasService;
import com.advocacia_microservice.plataforma.application.command.AlterarContrato;
import com.advocacia_microservice.plataforma.domain.repository.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.UsuarioRepository;
import com.advocacia_microservice.shared.exception.ConflitoException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.mock.web.MockHttpSession;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class GestaoEmpresasTests {
    @Autowired CriarUsuarioUseCase criar;
    @Autowired UsuarioRepository usuarios;
    @Autowired AdministracaoRepository admins;
    @Autowired ConviteInicialRepository convites;
    @Autowired EmpresaService empresas;
    @Autowired GestaoEmpresasService gestao;
    @Autowired WebApplicationContext context;
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    MockMvc mvc; UUID admin; UUID dono; UUID empresa;
    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        admin = criar.executar("Admin",UUID.randomUUID()+"@test.local").id(); admins.conceder(admin);
        dono = criar.executar("Responsável",UUID.randomUUID()+"@test.local").id();
        empresa = novaEmpresa(dono,"11222333000181");
    }
    UUID novaEmpresa(UUID usuario,String cnpj) {
        return EmpresaTestSupport.criar(empresas,convites,usuarios,usuario,Map.of("razao_social","Escritório Teste","cnpj",cnpj,"endereco","Rua A","cidade","Brasília")).id();
    }
    Set<String> modulos() { return gestao.buscar(empresa).modulos().stream().map(m -> m.codigo()).collect(java.util.stream.Collectors.toSet()); }
    void alterar(String situacao,Set<String> modulos) {
        gestao.alterar(empresa,new AlterarContrato(gestao.buscar(empresa).versao(),situacao,"INADIMPLENTE",null,modulos,"Medida administrativa"),admin);
        em.clear();
    }
    @Test void suspensaoRevogaSessaoAbertaPreservaOutraEmpresaEReativacaoNaoRestauraModuloRevogado() throws Exception {
        var funcionario = criar.executar("Funcionário",UUID.randomUUID()+"@test.local").id();
        jdbc.update("INSERT INTO empresa_usuario(usuario_id,empresa_id,papel) VALUES(?,?,'MEMBRO')",funcionario,empresa);
        var desativado = criar.executar("Desativado",UUID.randomUUID()+"@test.local").id();
        jdbc.update("INSERT INTO empresa_usuario(usuario_id,empresa_id,papel) VALUES(?,?,'MEMBRO')",desativado,empresa);
        jdbc.update("INSERT INTO equipe_membro(usuario_id,situacao) VALUES(?,'INATIVO')",desativado);
        var outro = criar.executar("Outro",UUID.randomUUID()+"@test.local").id(); novaEmpresa(outro,"22333444000182");
        var sessao = new MockHttpSession(); SecurityContext seguranca = SecurityContextHolder.createEmptyContext();
        seguranca.setAuthentication(new UsernamePasswordAuthenticationToken(dono.toString(),"",List.of()));
        sessao.setAttribute("SPRING_SECURITY_CONTEXT",seguranca);
        mvc.perform(get("/api/equipe/me").session(sessao)).andExpect(status().isOk());
        var permitidos = new HashSet<>(modulos()); permitidos.remove("contatos");
        alterar("SUSPENSA",permitidos);
        mvc.perform(get("/api/equipe/me").session(sessao)).andExpect(status().isUnauthorized());
        assertTrue(sessao.isInvalid());
        mvc.perform(get("/api/contatos").with(user(dono.toString()))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/equipe/me").with(user(funcionario.toString()))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/equipe/me").with(user(outro.toString()))).andExpect(status().isOk());
        alterar("ATIVA",permitidos);
        mvc.perform(get("/api/equipe/me").with(user(dono.toString()))).andExpect(status().isOk());
        mvc.perform(get("/api/equipe/me").with(user(funcionario.toString()))).andExpect(status().isOk());
        mvc.perform(get("/api/equipe/me").with(user(desativado.toString()))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/contatos").with(user(dono.toString()))).andExpect(status().isForbidden());
        assertEquals(empresa,empresas.buscar(dono).id());
        assertTrue(gestao.buscar(empresa).historico().stream().anyMatch(h -> h.acao().equals("EMPRESA_SUSPENSA")));
    }
    @Test void encerramentoPreservaCadastroEHistoricoEConflitoNaoAlteraContrato() {
        alterar("ENCERRADA",modulos());
        var antes = gestao.buscar(empresa);
        assertEquals("ENCERRADA",antes.situacao()); assertFalse(empresas.estaAtiva(empresa));
        assertThrows(ConflitoException.class,() -> gestao.alterar(empresa,new AlterarContrato(0,"ATIVA","REGULAR",null,modulos(),"Regularização"),admin));
        assertEquals(antes,gestao.buscar(empresa));
    }
    @Test void apenasPlataformaPodeAlterarEExigeCsrf() throws Exception {
        String dados = "{\"versao\":0,\"situacao\":\"SUSPENSA\",\"financeiro\":\"INADIMPLENTE\",\"modulos\":[\"inicio\",\"config\"],\"motivo\":\"Não pagamento\"}";
        var url = "/api/plataforma/admin/gestao-empresas/"+empresa;
        mvc.perform(put(url).with(user(dono.toString())).with(csrf()).contentType("application/json").content(dados)).andExpect(status().isForbidden());
        mvc.perform(put(url).with(user(admin.toString())).contentType("application/json").content(dados)).andExpect(status().isForbidden());
        mvc.perform(put(url).with(user(admin.toString())).with(csrf()).contentType("application/json").content(dados))
            .andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("SUSPENSA")).andExpect(jsonPath("$.versao").value(1));
    }
    @Test void filtrosPersistenciaEModulosNovosNaoSaoLiberadosAutomaticamente() {
        assertTrue(gestao.listar("Escritório", "ATIVA",0).stream().anyMatch(e -> e.id().equals(empresa)));
        assertTrue(gestao.listar("%", "",0).isEmpty());
        jdbc.update("INSERT INTO modulo_plataforma(codigo,nome,grupo,situacao,ordem) VALUES('novo','Novo','Principal','EM_DESENVOLVIMENTO',100)");
        assertFalse(gestao.modulosPermitidos(empresa).contains("novo"));
        alterar("SUSPENSA",modulos());
        assertTrue(gestao.listar("11222333000181","SUSPENSA",0).stream().anyMatch(e -> e.id().equals(empresa)));
        assertTrue(gestao.listar("11222333000181","ATIVA",0).isEmpty());
    }
}
