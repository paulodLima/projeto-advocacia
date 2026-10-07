package com.advocacia_microservice.usuario;

import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.StatusUsuario;
import com.advocacia_microservice.usuario.domain.Usuario;
import com.advocacia_microservice.usuario.infrastructure.persistence.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

@SpringBootTest
@ActiveProfiles("test")
class MeuPerfilTests {
    static final String URL = "/api/usuarios/me/perfil";
    @Autowired CriarUsuarioUseCase criar;
    @Autowired JpaUsuarioRepository usuarios;
    @Autowired JpaUsuarioPerfilRepository perfis;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    Usuario usuario;

    @BeforeEach void preparar() {
        // Os IDs e emails são exclusivos, sem modificar contas de outros testes.
        usuario = criar.executar("Perfil", java.util.UUID.randomUUID() + "@example.test");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void salvaConsultaLimpaEIsolaPerfilSemAlterarLogin() throws Exception {
        mvc.perform(get(URL).with(user(usuario.id().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.telefone").value(""));
        mvc.perform(put(URL).with(user(usuario.id().toString())).with(csrf()).contentType("application/json")
                .content("{\"telefone\":\" 11999999999 \",\"emailPessoal\":\"Contato@Example.test\",\"endereco\":\" Rua A \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.endereco").value("Rua A"))
                .andExpect(jsonPath("$.emailPessoal").value("contato@example.test"));
        mvc.perform(get(URL).with(user(usuario.id().toString())))
                .andExpect(jsonPath("$.telefone").value("11999999999"));
        assertEquals("Rua A", perfis.findById(usuario.id()).orElseThrow().paraDominio().endereco());
        assertEquals(usuario.email(), usuarios.findById(usuario.id()).orElseThrow().paraDominio().email());
        var outro = criar.executar("Outro", java.util.UUID.randomUUID() + "@example.test");
        mvc.perform(get(URL).with(user(outro.id().toString())))
                .andExpect(jsonPath("$.usuarioId").value(outro.id().toString()))
                .andExpect(jsonPath("$.endereco").value(""));
        mvc.perform(put(URL).with(user(usuario.id().toString())).with(csrf()).contentType("application/json")
                .content("{\"telefone\":\"\",\"emailPessoal\":\"\",\"endereco\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.telefone").value(""));
    }

    @Test void exigeSessaoCsrfEUsuarioAtivo() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(put(URL).with(user(usuario.id().toString())).contentType("application/json")
                .content("{}" )).andExpect(status().isForbidden());
        usuarios.saveAndFlush(UsuarioEntity.de(usuario.atualizar(usuario.nome(), usuario.email(), StatusUsuario.INATIVO)));
        mvc.perform(get(URL).with(user(usuario.id().toString()))).andExpect(status().isUnauthorized());
    }

    @Test void rejeitaDadosInvalidosSemGravar() throws Exception {
        mvc.perform(put(URL).with(user(usuario.id().toString())).with(csrf()).contentType("application/json")
                .content("{\"telefone\":\"" + "1".repeat(31) + "\",\"emailPessoal\":\"invalido\",\"endereco\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(URL).with(user(usuario.id().toString())).with(csrf()).contentType("application/json")
                .content("{}" )).andExpect(status().isBadRequest());
        assertFalse(perfis.existsById(usuario.id()));
    }

    private String imagem(int largura) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(largura, 1, BufferedImage.TYPE_INT_RGB), "png", output);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    }

    @Test void salvaRemoveFotoPreservaCamposEExcluiPerfilComUsuario() throws Exception {
        var foto = imagem(2);
        mvc.perform(put(URL + "/foto").with(user(usuario.id().toString())).with(csrf()).contentType("application/json")
                .content("{\"foto\":\"" + foto + "\"}" )).andExpect(status().isOk());
        mvc.perform(put(URL).with(user(usuario.id().toString())).with(csrf()).contentType("application/json")
                .content("{\"telefone\":\"123\",\"emailPessoal\":\"\",\"endereco\":\"Rua\"}"))
                .andExpect(jsonPath("$.foto").value(foto));
        mvc.perform(get(URL).with(user(usuario.id().toString()))).andExpect(jsonPath("$.foto").value(foto));
        mvc.perform(delete(URL + "/foto").with(user(usuario.id().toString())).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.foto").value(""))
                .andExpect(jsonPath("$.telefone").value("123"));
        usuarios.deleteById(usuario.id()); usuarios.flush();
        assertFalse(perfis.existsById(usuario.id()));
    }

    @Test void rejeitaFotoFalsaSvgGrandeEFormatoIncorreto() throws Exception {
        for (var foto : new String[] { "data:image/png;base64,YWJj", "data:image/svg+xml;base64,YWJj",
                imagem(513), imagem(2).replace("image/png", "image/jpeg") }) {
            mvc.perform(put(URL + "/foto").with(user(usuario.id().toString())).with(csrf()).contentType("application/json")
                    .content("{\"foto\":\"" + foto + "\"}" )).andExpect(status().isBadRequest());
        }
        assertFalse(perfis.existsById(usuario.id()));
    }
}
