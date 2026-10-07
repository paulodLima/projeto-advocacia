package com.advocacia_microservice.usuario.application;

import com.advocacia_microservice.shared.exception.RecursoNaoEncontradoException;
import com.advocacia_microservice.usuario.infrastructure.persistence.JpaUsuarioRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemaUsuarioService {
    public record Preferencia(UUID usuarioId, String tema) {}
    private static final Set<String> TEMAS = Set.of("verde", "vermelho", "amarelo", "azul", "preto", "roxo");
    private final JdbcTemplate jdbc;
    private final JpaUsuarioRepository usuarios;
    public TemaUsuarioService(JdbcTemplate jdbc, JpaUsuarioRepository usuarios) { this.jdbc=jdbc; this.usuarios=usuarios; }
    @Transactional(readOnly=true)
    public Preferencia buscar(UUID id) {
        if (!usuarios.existsById(id)) throw new RecursoNaoEncontradoException("Usuário não encontrado.");
        String tema=jdbc.query("SELECT tema FROM usuario_tema WHERE usuario_id=?",(rs,n)->rs.getString(1),id).stream().findFirst().orElse("verde");
        return new Preferencia(id,tema);
    }
    @Transactional
    public Preferencia salvar(UUID id, String tema) {
        if (tema==null || !TEMAS.contains(tema)) throw new IllegalArgumentException("Escolha um dos temas disponíveis.");
        usuarios.buscarParaAtualizar(id).orElseThrow(()->new RecursoNaoEncontradoException("Usuário não encontrado."));
        jdbc.update("DELETE FROM usuario_tema WHERE usuario_id=?",id);
        jdbc.update("INSERT INTO usuario_tema(usuario_id,tema) VALUES (?,?)",id,tema);
        return new Preferencia(id,tema);
    }
}
