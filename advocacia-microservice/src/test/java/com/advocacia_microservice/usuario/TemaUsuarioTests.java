package com.advocacia_microservice.usuario;

import com.advocacia_microservice.usuario.application.*;
import com.advocacia_microservice.usuario.domain.Usuario;
import com.advocacia_microservice.usuario.infrastructure.persistence.JpaUsuarioRepository;
import com.advocacia_microservice.shared.exception.RecursoNaoEncontradoException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test")
class TemaUsuarioTests {
    @Autowired TemaUsuarioService service;
    @Autowired CriarUsuarioUseCase criarUsuario;
    @Autowired JpaUsuarioRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    Usuario usuario; MockMvc mvc;
    Usuario novo() { return criarUsuario.executar("Teste",UUID.randomUUID()+"@example.test"); }
    @BeforeEach void preparar() { usuario=novo();mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }
    @Test void padraoVerdeESeisCoresPersistemParaContaSemEmpresa() throws Exception {
        mvc.perform(get("/api/usuarios/me/tema").with(user(usuario.id().toString()))).andExpect(status().isOk()).andExpect(jsonPath("$.tema").value("verde"));
        for(var tema:List.of("verde","vermelho","amarelo","azul","preto","roxo")) {
            mvc.perform(put("/api/usuarios/me/tema").with(user(usuario.id().toString())).with(csrf()).contentType("application/json").content("{\"tema\":\""+tema+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tema").value(tema)).andExpect(jsonPath("$.usuarioId").value(usuario.id().toString()));
            assertEquals(tema,service.buscar(usuario.id()).tema());
        }
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM usuario_tema WHERE usuario_id=?",Integer.class,usuario.id()));
    }
    @Test void isolaContasEUsaSomenteUsuarioDaSessao() throws Exception {
        var outro=novo();service.salvar(outro.id(),"roxo");
        mvc.perform(put("/api/usuarios/me/tema").with(user(usuario.id().toString())).with(csrf()).contentType("application/json").content("{\"tema\":\"azul\",\"usuarioId\":\""+outro.id()+"\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.usuarioId").value(usuario.id().toString()));
        assertEquals("azul",service.buscar(usuario.id()).tema());assertEquals("roxo",service.buscar(outro.id()).tema());
    }
    @Test void exigeSessaoECsrfERejeitaCoresInvalidasSemAlterarPreferencia() throws Exception {
        service.salvar(usuario.id(),"amarelo");
        mvc.perform(get("/api/usuarios/me/tema")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/usuarios/me/tema").with(user(usuario.id().toString())).contentType("application/json").content("{\"tema\":\"roxo\"}")).andExpect(status().isForbidden());
        for(var body:List.of("{}","{\"tema\":\"\"}","{\"tema\":\"invalido\"}","{\"tema\":\"AZUL\"}"))
            mvc.perform(put("/api/usuarios/me/tema").with(user(usuario.id().toString())).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        assertEquals("amarelo",service.buscar(usuario.id()).tema());
    }
    @Test void removePreferenciaAoExcluirUsuarioENaoCriaParaIdInexistente() {
        service.salvar(usuario.id(),"preto");usuarios.deleteById(usuario.id());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM usuario_tema WHERE usuario_id=?",Integer.class,usuario.id()));
        assertThrows(RecursoNaoEncontradoException.class,()->service.buscar(usuario.id()));
        assertThrows(RecursoNaoEncontradoException.class,()->service.salvar(UUID.randomUUID(),"azul"));
    }
}
