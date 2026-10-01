package com.advocacia_microservice.usuario;

import com.advocacia_microservice.shared.exception.ConflitoException;
import com.advocacia_microservice.shared.exception.RecursoNaoEncontradoException;
import com.advocacia_microservice.usuario.application.*;
import com.advocacia_microservice.usuario.domain.StatusUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UsuarioUseCasesTests {
    @Autowired CriarUsuarioUseCase criar;
    @Autowired BuscarUsuarioUseCase buscar;
    @Autowired AtualizarUsuarioUseCase atualizar;
    @Autowired ExcluirUsuarioUseCase excluir;

    @Test
    void criaBuscaAtualizaEExcluiUsuario() {
        var usuario = criar.executar(" Maria ", "MARIA@example.com");
        assertEquals("Maria", buscar.executar(usuario.id()).nome());
        assertEquals("maria@example.com", usuario.email());
        assertEquals(StatusUsuario.ATIVO, usuario.status());
        var alterado = atualizar.executar(usuario.id(), "Maria Silva", usuario.email(), StatusUsuario.INATIVO);
        assertEquals(usuario.id(), alterado.id());
        assertEquals("Maria Silva", buscar.executar(usuario.id()).nome());
        assertEquals(StatusUsuario.INATIVO, buscar.executar(usuario.id()).status());
        excluir.executar(usuario.id());
        assertThrows(RecursoNaoEncontradoException.class, () -> buscar.executar(usuario.id()));
    }

    @Test
    void rejeitaEmailDuplicadoNaCriacaoEAtualizacao() {
        var primeiro = criar.executar("Maria", "maria@example.com");
        assertThrows(ConflitoException.class, () -> criar.executar("Outra Maria", "MARIA@example.com"));
        var segundo = criar.executar("João", "joao@example.com");
        assertThrows(ConflitoException.class, () -> atualizar.executar(
                segundo.id(), "João", primeiro.email(), StatusUsuario.ATIVO));
    }
}
