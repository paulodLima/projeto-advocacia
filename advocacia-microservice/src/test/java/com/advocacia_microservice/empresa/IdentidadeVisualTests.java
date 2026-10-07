package com.advocacia_microservice.empresa;

import com.advocacia_microservice.empresa.application.*;
import com.advocacia_microservice.empresa.domain.*;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.domain.Usuario;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
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
class IdentidadeVisualTests {
    @Autowired EmpresaService empresas;
    @Autowired EmpresaRepository repository;
    @Autowired IdentidadeVisualService service;
    @Autowired CriarUsuarioUseCase criarUsuario;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    Usuario master;
    UUID empresa;
    @BeforeEach void preparar() {
        master = novo();
        empresa = empresas.criar(master.id(), Map.of("razao_social", "Escritório Teste", "cnpj", UUID.randomUUID().toString().substring(0, 14), "endereco", "Rua A", "cidade", "São Paulo")).id();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    Usuario novo() { return criarUsuario.executar("Teste", UUID.randomUUID() + "@example.test"); }
    String imagem(int tamanho) throws Exception {
        var saida = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(tamanho, 2, BufferedImage.TYPE_INT_ARGB), "png", saida);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(saida.toByteArray());
    }
    String body(String imagem) {
        return "{\"imagens\":{\"foto_login\":\"" + imagem + "\",\"logo_simples_branca\":\"" + imagem + "\"},\"usos\":{\"login\":\"logo_simples_branca\"},\"zoom\":1.7,\"posX\":20,\"posY\":70}";
    }
    @Test void salvaRestauraPublicaERemoveSemExporDadosDaEmpresa() throws Exception {
        mvc.perform(put("/api/empresas/minha/identidade").with(user(master.id().toString())).with(csrf()).contentType("application/json").content(body(imagem(3))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.identidade.zoom").value(1.7));
        mvc.perform(get("/api/empresas/minha/identidade").with(user(master.id().toString())))
            .andExpect(jsonPath("$.identidade.posX").value(20)).andExpect(jsonPath("$.identidade.usos.login").value("logo_simples_branca"));
        mvc.perform(get("/api/public/empresas/" + empresa + "/identidade"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Escritório Teste"))
            .andExpect(jsonPath("$.dados").doesNotExist()).andExpect(jsonPath("$.papel").doesNotExist())
            .andExpect(jsonPath("$.identidade.imagens.foto_login").isString());
        service.salvar(master.id(), IdentidadeVisual.padrao());
        assertTrue(service.buscar(master.id()).identidade().imagens().isEmpty());
        assertTrue(service.publica(empresa).identidade().usos().isEmpty());
    }
    @Test void membroConsultaMasNaoAlteraEOutraEmpresaNaoRecebeAsImagens() throws Exception {
        service.salvar(master.id(), new IdentidadeVisual(Map.of("foto_login", imagem(2)), Map.of(), 1, 50, 50));
        var membro = novo(); repository.vincular(new VinculoEmpresa(membro.id(), empresa, PapelEmpresa.MEMBRO));
        mvc.perform(get("/api/empresas/minha/identidade").with(user(membro.id().toString())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.empresaId").value(empresa.toString()));
        mvc.perform(put("/api/empresas/minha/identidade").with(user(membro.id().toString())).with(csrf()).contentType("application/json").content(body(imagem(2))))
            .andExpect(status().isForbidden());
        var outro = novo(); empresas.criar(outro.id(), Map.of("razao_social", "Outro", "cnpj", UUID.randomUUID().toString().substring(0, 14), "endereco", "Rua B", "cidade", "São Paulo"));
        assertTrue(service.buscar(outro.id()).identidade().imagens().isEmpty());
    }
    @Test void exigeAutenticacaoCsrfEVinculoEConsultaPublicaInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/empresas/minha/identidade")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/empresas/minha/identidade").with(user(master.id().toString())).contentType("application/json").content(body(imagem(2))))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/empresas/minha/identidade").with(user(novo().id().toString()))).andExpect(status().isNotFound());
        mvc.perform(get("/api/public/empresas/" + UUID.randomUUID() + "/identidade")).andExpect(status().isNotFound());
    }
    @Test void rejeitaArquivosInvalidosDimensoesUsosECortesSemModificarOBanco() throws Exception {
        var valido = body(imagem(2));
        for (var invalido : List.of(body("data:image/png;base64,YWJj"), body("data:image/svg+xml;base64,PHN2Zz4="), body(imagem(1401)),
                valido.replace("1.7", "3"), valido.replace("\"posX\":20", "\"posX\":-1"),
                valido.replace("\"login\":\"logo_simples_branca\"", "\"login\":\"logo_completa_branca\""),
                valido.replace("foto_login", "tipo_invalido"))) {
            mvc.perform(put("/api/empresas/minha/identidade").with(user(master.id().toString())).with(csrf()).contentType("application/json").content(invalido))
                .andExpect(status().isBadRequest());
            assertTrue(service.buscar(master.id()).identidade().imagens().isEmpty());
        }
        assertThrows(IllegalArgumentException.class, () -> new IdentidadeVisual(Map.of("foto_login", "data:image/png;base64," + "A".repeat(2_796_241)), Map.of(), 1, 50, 50));
    }
}
